package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowEpisodeCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    private val cache =
        MapCache<UUIDPaginationKeyCache, PageableDto<EpisodeMappingDto>>(
            "MemberFollowEpisodeCacheService.cache",
            classes = listOf(
                MemberFollowEpisode::class.java,
            ),
        ) {
            val member = memberCacheService.find(it.uuid) ?: return@MapCache PageableDto.empty()

            val pageable = memberFollowEpisodeService.findAllFollowedEpisodes(
                member,
                it.page,
                it.limit
            )

            PageableDto.fromPageable(pageable, EpisodeMappingDto::class.java)
        }

    fun findAllBy(member: UUID, page: Int, limit: Int) = cache[UUIDPaginationKeyCache(member, page, limit)]
}