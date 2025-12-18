package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils

class SimulcastCacheService : ICacheService {
    @Inject private lateinit var simulcastService: SimulcastService

    fun findAll() = MapCache.getOrCompute(
        "SimulcastCacheService.findAll",
        classes = listOf(Simulcast::class.java, Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<SimulcastDto>>>() {},
        key = StringUtils.EMPTY_STRING
    ) { simulcastService.findAllModified().toTypedArray() }

    val currentSimulcast: SimulcastDto?
        get() = findAll().firstOrNull()
}