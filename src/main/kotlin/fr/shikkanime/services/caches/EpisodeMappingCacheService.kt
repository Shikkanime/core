package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeUUIDSeasonSortPaginationKeyCache
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.MapCache
import java.util.*

class EpisodeMappingCacheService : AbstractCacheService {
    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var animeService: AnimeService

    private val findAllByCache =
        MapCache<CountryCodeUUIDSeasonSortPaginationKeyCache, PageableDto<EpisodeMappingDto>>(
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java
            )
        ) {
            PageableDto.fromPageable(
                episodeMappingService.findAllBy(
                    it.countryCode,
                    animeService.find(it.uuid),
                    it.season,
                    it.sort,
                    it.page,
                    it.limit,
                    it.status
                ),
                EpisodeMappingDto::class.java
            )
        }

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = findAllByCache[CountryCodeUUIDSeasonSortPaginationKeyCache(countryCode, anime, season, sort, page, limit, status)]
}