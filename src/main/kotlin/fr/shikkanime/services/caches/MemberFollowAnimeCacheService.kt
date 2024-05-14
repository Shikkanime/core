package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.MissedAnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberFollowEpisodeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowAnimeCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    private val cache =
        MapCache<UUIDPaginationKeyCache, PageableDto<MissedAnimeDto>?>(
            classes = listOf(
                MemberFollowAnime::class.java,
                MemberFollowEpisode::class.java,
                EpisodeMapping::class.java,
            ),
        ) {
            val member = memberCacheService.find(it.uuid) ?: return@MapCache null

            val pageable = memberFollowAnimeService.findAllMissedAnimes(
                member,
                memberFollowEpisodeService.findAllFollowedEpisodesUUID(member),
                it.page,
                it.limit
            )

            val dtos = pageable.data.map { tuple ->
                MissedAnimeDto(
                    AbstractConverter.convert(tuple[0], AnimeDto::class.java),
                    tuple[1] as Long
                )
            }

            PageableDto(
                data = dtos,
                page = pageable.page,
                limit = pageable.limit,
                total = pageable.total,
            )
        }

    fun getMissedAnimes(member: UUID, page: Int, limit: Int) = cache[UUIDPaginationKeyCache(member, page, limit)]
}