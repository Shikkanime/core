package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace
import java.util.*

class MemberFollowEpisodeCacheService : ICacheService {
    private val tracer = TelemetryConfig.getTracer("MemberFollowEpisodeCacheService")
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    fun findAllBy(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowEpisodeCacheService.findAllBy",
        classes = listOf(EpisodeMapping::class.java, MemberFollowEpisode::class.java),
        key = UUIDPaginationKeyCache(member, page, limit),
    ) { tracer.trace {
        PageableDto.fromPageable(
            memberFollowEpisodeService.findAllFollowedEpisodes(
                memberCacheService.find(it.uuid) ?: return@trace PageableDto.empty(),
                it.page,
                it.limit
            ),
            episodeMappingFactory
        )
    } }
}