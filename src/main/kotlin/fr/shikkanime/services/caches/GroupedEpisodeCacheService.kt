package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodePaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.factories.impl.GroupedEpisodeFactory
import fr.shikkanime.services.GroupedEpisodeService
import fr.shikkanime.utils.MapCache

class GroupedEpisodeCacheService : ICacheService {
    @Inject private lateinit var groupedEpisodeService: GroupedEpisodeService
    @Inject private lateinit var groupedEpisodeFactory: GroupedEpisodeFactory

    fun findAllBy(
        countryCode: CountryCode,
        page: Int,
        limit: Int,
    ) = MapCache.getOrCompute(
        "GroupedEpisodeCacheService.findAllBy",
        classes = listOf(),
        key = CountryCodePaginationKeyCache(countryCode, page, limit),
    ) {
        PageableDto.fromPageable(
            groupedEpisodeService.findAllBy(
                it.countryCode!!,
                it.page,
                it.limit,
            ),
            groupedEpisodeFactory,
        )
    }
}