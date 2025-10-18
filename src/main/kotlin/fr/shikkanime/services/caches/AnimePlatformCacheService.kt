package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.AnimePlatformFactory
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import java.util.*

class AnimePlatformCacheService : ICacheService {
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var animePlatformFactory: AnimePlatformFactory

    fun getAll(anime: Anime) = MapCache.getOrCompute(
        "AnimePlatformCacheService.getAll",
        classes = listOf(Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<AnimePlatformDto>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = anime.uuid!!,
    ) { uuid -> animePlatformService.findAllByAnime(uuid).map(animePlatformFactory::toDto).toTypedArray() }

    fun findAllIdByAnimeAndPlatform(animeUuid: UUID, platform: Platform) = MapCache.getOrCompute(
        "AnimePlatformCacheService.findAllIdByAnimeAndPlatform",
        classes = listOf(UUID::class.java, Platform::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        key = animeUuid to platform,
    ) { (animeUuid, platform) -> animePlatformService.findAllIdByAnimeAndPlatform(animeUuid, platform).toTypedArray() }
}