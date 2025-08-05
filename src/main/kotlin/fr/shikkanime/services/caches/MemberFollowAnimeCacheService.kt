package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.MissedAnimeFactory
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowAnimeCacheService : ICacheService {
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var missedAnimeFactory: MissedAnimeFactory

    fun getMissedAnimes(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowAnimeCacheService.getMissedAnimes",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, MemberFollowAnime::class.java, MemberFollowEpisode::class.java),
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        PageableDto.fromPageable(
            memberFollowAnimeService.findAllMissedAnimes(
                memberCacheService.find(it.uuid) ?: return@getOrCompute PageableDto.empty(),
                it.page,
                it.limit
            ),
            missedAnimeFactory
        )
    }

    fun findAllBy(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowAnimeCacheService.findAllBy",
        classes = listOf(Anime::class.java, MemberFollowAnime::class.java),
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        PageableDto.fromPageable(
            memberFollowAnimeService.findAllFollowedAnimes(
                memberCacheService.find(it.uuid) ?: return@getOrCompute PageableDto.empty(),
                it.page,
                it.limit
            ),
            animeFactory
        )
    }

    fun existsByMemberAndAnime(
        memberUuid: UUID,
        animeUuid: UUID
    ) = MapCache.getOrCompute(
        "MemberFollowAnimeCacheService.existsByMemberAndAnime",
        classes = listOf(Anime::class.java, MemberFollowAnime::class.java),
        key = memberUuid to animeUuid,
    ) { memberFollowAnimeService.existsByMemberAndAnime(it.first, it.second) }
}