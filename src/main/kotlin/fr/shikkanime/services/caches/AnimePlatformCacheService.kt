package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.AnimePlatformFactory
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import java.util.*

class AnimePlatformCacheService : ICacheService {
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var animePlatformFactory: AnimePlatformFactory

    suspend fun findAllByAnime(anime: Anime) = MapCache.getOrComputeAsync(
        "AnimePlatformCacheService.findAllByAnime",
        classes = listOf(Anime::class.java, AnimePlatform::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<AnimePlatformDto>>>() {},
        key = anime.uuid!!,
    ) { uuid -> animePlatformService.findAllByAnime(uuid).map { animePlatformFactory.toDto(it) }.toTypedArray() }

    suspend fun findAllByPlatform(platform: Platform) = MapCache.getOrComputeAsync(
        "AnimePlatformCacheService.findAllByPlatform",
        classes = listOf(AnimePlatform::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<AnimePlatformDto>>>() {},
        key = platform,
    ) { platform -> animePlatformService.findAllByPlatform(platform).map { animePlatformFactory.toDto(it) }.toTypedArray() }

    suspend fun findAllIdByAnimeAndPlatform(animeUuid: UUID, platform: Platform) = MapCache.getOrComputeAsync(
        "AnimePlatformCacheService.findAllIdByAnimeAndPlatform",
        classes = listOf(Anime::class.java, AnimePlatform::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        key = animeUuid to platform,
    ) { (animeUuid, platform) -> animePlatformService.findAllIdByAnimeAndPlatform(animeUuid, platform).toTypedArray() }
}