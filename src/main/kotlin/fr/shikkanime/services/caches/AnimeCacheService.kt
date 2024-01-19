package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.MapCache
import java.util.*

class AnimeCacheService : AbstractCacheService() {
    @Inject
    private lateinit var animeService: AnimeService

    private val cache = MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
        PageableDto.fromPageable(animeService.findAllBy(it.countryCode, it.uuid, it.sort, it.page, it.limit), AnimeDto::class.java)
    }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = cache[CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit)]
}