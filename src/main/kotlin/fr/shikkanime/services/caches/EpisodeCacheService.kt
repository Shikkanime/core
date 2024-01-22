package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.MapCache
import java.util.*

class EpisodeCacheService : AbstractCacheService() {
    @Inject
    private lateinit var episodeService: EpisodeService

    private val cache = MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<EpisodeDto>>(classes = listOf(Episode::class.java)) {
        PageableDto.fromPageable(episodeService.findAllBy(it.countryCode, it.uuid, it.sort, it.page, it.limit), EpisodeDto::class.java)
    }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = cache[CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit)]
}