package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import fr.shikkanime.utils.StringUtils
import java.util.*

class SimulcastCacheService : ICacheService {
    @Inject private lateinit var simulcastService: SimulcastService

    fun findAll() = MapCache.getOrCompute(
        "SimulcastCacheService.findAll",
        classes = listOf(Simulcast::class.java, Anime::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<SimulcastDto>>>() {},
        key = StringUtils.EMPTY_STRING
    ) { simulcastService.findAllModified().toTypedArray() }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "SimulcastCacheService.find",
        classes = listOf(Simulcast::class.java),
        typeToken = object : TypeToken<MapCacheValue<Simulcast>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = uuid
    ) { simulcastService.find(it) }

    val currentSimulcast: SimulcastDto?
        get() = findAll().firstOrNull()
}