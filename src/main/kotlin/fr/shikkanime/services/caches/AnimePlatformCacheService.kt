package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import fr.shikkanime.utils.StringUtils
import java.util.*

class AnimePlatformCacheService : ICacheService {
    @Inject private lateinit var animePlatformService: AnimePlatformService

    fun getAll(anime: Anime) = MapCache.getOrCompute(
        "AnimePlatformCacheService.getAll",
        classes = listOf(Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<HashMap<UUID, List<AnimePlatform>>>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = StringUtils.EMPTY_STRING,
    ) { HashMap(animePlatformService.findAll().groupBy { it.anime!!.uuid!! }) }[anime.uuid] ?: emptyList()
}