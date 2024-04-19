package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeLocalDateKeyCache
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import java.time.LocalDate
import java.util.*

class AnimeCacheService : AbstractCacheService {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    private val findAllByCache =
        MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
            PageableDto.fromPageable(
                animeService.findAllBy(
                    it.countryCode,
                    simulcastService.find(it.uuid),
                    it.sort,
                    it.page,
                    it.limit,
                    it.status
                ),
                AnimeDto::class.java
            )
        }

    private val findAllByNameCache =
        MapCache<CountryCodeNamePaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
            PageableDto.fromPageable(
                animeService.findAllByName(it.name, it.countryCode, it.page, it.limit),
                AnimeDto::class.java
            )
        }

    private val findBySlugCache = MapCache<String, Anime?>(classes = listOf(Anime::class.java)) {
        animeService.findBySlug(it)
    }

    private val weeklyCache =
        MapCache<CountryCodeLocalDateKeyCache, List<WeeklyAnimesDto>>(classes = listOf(EpisodeMapping::class.java, EpisodeVariant::class.java)) {
            animeService.getWeeklyAnimes(it.localDate, it.countryCode)
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

    fun findBySlug(slug: String) = findBySlugCache[slug]

    fun getWeeklyAnimes(startOfWeekDay: LocalDate, countryCode: CountryCode) =
        weeklyCache[CountryCodeLocalDateKeyCache(countryCode, startOfWeekDay)]
}