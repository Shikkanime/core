package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowEpisodeCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    fun findAllBy(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowEpisodeCacheService.findAllBy",
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        PageableDto.fromPageable(
            memberFollowEpisodeService.findAllFollowedEpisodes(
                memberCacheService.find(it.uuid) ?: return@getOrCompute PageableDto.empty(),
                it.page,
                it.limit
            ),
            EpisodeMappingDto::class.java
        )
    }
}