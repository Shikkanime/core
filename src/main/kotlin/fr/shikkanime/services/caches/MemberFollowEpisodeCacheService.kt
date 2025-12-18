package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import java.util.*

class MemberFollowEpisodeCacheService : ICacheService {
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    fun findAllBy(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowEpisodeCacheService.findAllBy",
        classes = listOf(EpisodeMapping::class.java, MemberFollowEpisode::class.java),
        typeToken = object : TypeToken<MapCacheValue<PageableDto<EpisodeMappingDto>>>() {},
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        PageableDto.fromPageable(
            memberFollowEpisodeService.findAllFollowedEpisodes(
                it.uuid,
                it.page,
                it.limit
            ),
           episodeMappingFactory
        )
    }
}