package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.factories.impl.AnimePlatformFactory
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils

class AnimePlatformCacheService : ICacheService {
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var animePlatformFactory: AnimePlatformFactory

    fun getAll(anime: Anime) = MapCache.getOrCompute(
        "AnimePlatformCacheService.getAll",
        classes = listOf(Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<AnimePlatformDto>>>() {},
        serializationType = SerializationUtils.SerializationType.JSON,
        key = anime.uuid!!,
    ) { uuid -> animePlatformService.findAllByAnime(uuid).map { animePlatformFactory.toDto(it) }.toTypedArray() }
}