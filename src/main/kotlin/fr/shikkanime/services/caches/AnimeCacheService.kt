package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.animes.DetailedAnimeDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import java.time.LocalDate
import java.util.*

class AnimeCacheService : AbstractCacheService {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    private val findAllByCache =
        MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<DetailedAnimeDto>>(classes = listOf(Anime::class.java)) {
            PageableDto.fromPageable(
                animeService.findAllBy(
                    it.countryCode,
                    simulcastService.find(it.uuid),
                    it.sort,
                    it.page,
                    it.limit,
                    it.status
                ),
                DetailedAnimeDto::class.java
            )
        }

    private val findAllByNameCache =
        MapCache<CountryCodeNamePaginationKeyCache, PageableDto<DetailedAnimeDto>>(classes = listOf(Anime::class.java)) {
            PageableDto.fromPageable(
                animeService.findAllByName(it.name, it.countryCode, it.page, it.limit),
                DetailedAnimeDto::class.java
            )
        }

    private val findBySlugCache = MapCache<CountryCodeIdKeyCache, DetailedAnimeDto?>(classes = listOf(Anime::class.java, EpisodeMapping::class.java)) {
        animeService.findBySlug(it.countryCode, it.id)
            .let { anime -> AbstractConverter.convert(anime, DetailedAnimeDto::class.java) }
    }

    private val findAllCache = MapCache<String, List<DetailedAnimeDto>>(
        classes = listOf(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java
        )
    ) {
        val list = animeService.findAllLoaded()
        val dtos = list.associateWith { AbstractConverter.convert(it, DetailedAnimeDto::class.java) }

        dtos.forEach { (anime, dto) ->
            dto.episodes = AbstractConverter.convert(
                episodeMappingService.findAllByAnime(anime),
                EpisodeMappingWithoutAnimeDto::class.java
            )
        }

        dtos.values.toList()
    }

    private val weeklyMemberCache =
        MapCache<CountryCodeLocalDateKeyCache, List<WeeklyAnimesDto>>(
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                MemberFollowAnime::class.java
            )
        ) {
            animeService.getWeeklyAnimes(it.member?.let { uuid -> memberService.find(uuid) }, it.localDate, it.countryCode)
        }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null,
    ) = findAllByCache[CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit, status)]

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int) =
        findAllByNameCache[CountryCodeNamePaginationKeyCache(countryCode, name, page, limit)]

    fun findBySlug(countryCode: CountryCode, slug: String) = findBySlugCache[CountryCodeIdKeyCache(countryCode, slug)]

    fun getWeeklyAnimes(member: UUID?, startOfWeekDay: LocalDate, countryCode: CountryCode) =
        weeklyMemberCache[CountryCodeLocalDateKeyCache(member, countryCode, startOfWeekDay)]

    fun findAll() = findAllCache["all"]
}