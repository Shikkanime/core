package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.utils.MapCache
import java.util.*

class MemberFollowAnimeCacheService : AbstractCacheService {
    @Inject
    private lateinit var memberCacheService: MemberCacheService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    fun getMissedAnimes(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowAnimeCacheService.getMissedAnimes",
        classes = listOf(MemberFollowAnime::class.java, MemberFollowEpisode::class.java, Anime::class.java, EpisodeMapping::class.java),
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        val pageable = memberFollowAnimeService.findAllMissedAnimes(
            memberCacheService.find(it.uuid) ?: return@getOrCompute PageableDto.empty(),
            it.page,
            it.limit
        )

        val dtos = pageable.data.map { tuple ->
            MissedAnimeDto(
                AbstractConverter.convert(tuple[0], AnimeDto::class.java),
                tuple[1] as Long
            )
        }.toSet()

        PageableDto(
            data = dtos,
            page = pageable.page,
            limit = pageable.limit,
            total = pageable.total,
        )
    }

    fun findAllBy(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowAnimeCacheService.findAllBy",
        classes = listOf(MemberFollowAnime::class.java, Anime::class.java),
        key = UUIDPaginationKeyCache(member, page, limit),
    ) {
        PageableDto.fromPageable(
            memberFollowAnimeService.findAllFollowedAnimes(
                memberCacheService.find(it.uuid) ?: return@getOrCompute PageableDto.empty(),
                it.page,
                it.limit
            ),
            AnimeDto::class.java
        )
    }
}