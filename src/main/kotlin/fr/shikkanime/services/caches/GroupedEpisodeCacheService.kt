package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeSortPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.GroupedEpisodeFactory
import fr.shikkanime.services.GroupedEpisodeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue

class GroupedEpisodeCacheService : ICacheService {
    @Inject private lateinit var groupedEpisodeService: GroupedEpisodeService
    @Inject private lateinit var groupedEpisodeFactory: GroupedEpisodeFactory

    fun findAllBy(
        countryCode: CountryCode?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = MapCache.getOrCompute(
        "GroupedEpisodeCacheService.findAllBy",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java),
        typeToken = object : TypeToken<MapCacheValue<PageableDto<GroupedEpisodeDto>>>() {},
        key = CountryCodeSortPaginationKeyCache(countryCode, sort,page, limit),
    ) {
        PageableDto.fromPageable(
            groupedEpisodeService.findAllBy(it.countryCode, it.sort, it.page, it.limit),
            groupedEpisodeFactory
        )
    }
}