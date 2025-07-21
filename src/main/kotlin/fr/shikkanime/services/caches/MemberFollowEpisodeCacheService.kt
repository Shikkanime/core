package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowEpisodeCacheService : ICacheService {
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    fun findAllBy(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowEpisodeCacheService.findAllBy",
        classes = listOf(EpisodeMapping::class.java, MemberFollowEpisode::class.java),
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        PageableDto.fromPageable(
            memberFollowEpisodeService.findAllFollowedEpisodes(
                memberCacheService.find(it.uuid) ?: return@getOrCompute PageableDto.empty(),
                it.page,
                it.limit
            ),
           episodeMappingFactory
        )
    }

    fun existsByMemberAndEpisode(
        memberUuid: UUID,
        episodeUuid: UUID
    ) = MapCache.getOrCompute(
        "MemberFollowEpisodeCacheService.existsByMemberAndEpisode",
        classes = listOf(EpisodeMapping::class.java, MemberFollowEpisode::class.java),
        key = memberUuid to episodeUuid,
    ) { memberFollowEpisodeService.existsByMemberAndEpisode(it.first, it.second) }
}