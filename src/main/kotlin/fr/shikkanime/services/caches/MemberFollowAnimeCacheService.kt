package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.caches.UUIDPaginationKeyCache
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.MissedAnimeFactory
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import java.util.*

class MemberFollowAnimeCacheService : ICacheService {
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var missedAnimeFactory: MissedAnimeFactory

    fun getMissedAnimes(member: UUID, page: Int, limit: Int) = MapCache.getOrCompute(
        "MemberFollowAnimeCacheService.getMissedAnimes",
        classes = listOf(Anime::class.java, EpisodeMapping::class.java, MemberFollowAnime::class.java, MemberFollowEpisode::class.java),
        typeToken = object : TypeToken<MapCacheValue<PageableDto<MissedAnimeDto>>>() {},
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
        typeToken = object : TypeToken<MapCacheValue<PageableDto<AnimeDto>>>() {},
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
}