package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
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
        MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
            PageableDto.fromPageable(
                animeService.findAllBy(
                    it.countryCode,
                    simulcastService.find(it.uuid),
                    it.sort,
                    it.page,
                    it.limit,
                    it.searchTypes,
                    it.status
                ),
                AnimeDto::class.java
            )
        }

    private val findAllByNameCache =
        MapCache<CountryCodeNamePaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
            try {
                PageableDto.fromPageable(
                    animeService.findAllByName(it.countryCode, it.name, it.page, it.limit, it.searchTypes),
                    AnimeDto::class.java
                )
            } catch (e: Exception) {
                e.printStackTrace()
                PageableDto.empty()
            }
        }

    private val findBySlugCache = MapCache<CountryCodeIdKeyCache, AnimeDto?>(classes = listOf(Anime::class.java, EpisodeMapping::class.java)) {
        animeService.findBySlug(it.countryCode, it.id)
            .let { anime -> AbstractConverter.convert(anime, AnimeDto::class.java) }
    }

    private val findAllCache = MapCache<String, List<AnimeDto>>(
        classes = listOf(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java
        )
    ) {
        val list = animeService.findAllLoaded()
        val dtos = list.associateWith { AbstractConverter.convert(it, AnimeDto::class.java) }

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
            animeService.getWeeklyAnimes(
                it.countryCode,
                it.member?.let { uuid -> memberService.find(uuid) },
                it.localDate
            )
        }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        searchTypes: List<LangType>? = null,
        status: Status? = null,
    ) = findAllByCache[CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit, searchTypes, status)]

    fun findAllByName(countryCode: CountryCode?, name: String, page: Int, limit: Int, searchTypes: List<LangType>?) =
        findAllByNameCache[CountryCodeNamePaginationKeyCache(countryCode, name, page, limit, searchTypes)]

    fun findBySlug(countryCode: CountryCode, slug: String) = findBySlugCache[CountryCodeIdKeyCache(countryCode, slug)]

    fun getWeeklyAnimes(countryCode: CountryCode, memberUuid: UUID?, startOfWeekDay: LocalDate) =
        weeklyMemberCache[CountryCodeLocalDateKeyCache(memberUuid, countryCode, startOfWeekDay)]

    fun findAll() = findAllCache["all"]
}