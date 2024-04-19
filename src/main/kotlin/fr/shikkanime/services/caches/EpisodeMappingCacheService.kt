package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
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
        MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<EpisodeMappingDto>>(
            classes = listOf(
                EpisodeMapping::class.java,
                EpisodeVariant::class.java
            )
        ) {
            PageableDto.fromPageable(
                episodeMappingService.findAllBy(
                    it.countryCode,
                    animeService.find(it.uuid),
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
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = findAllByCache[CountryCodeUUIDSortPaginationKeyCache(countryCode, anime, sort, page, limit, status)]
}