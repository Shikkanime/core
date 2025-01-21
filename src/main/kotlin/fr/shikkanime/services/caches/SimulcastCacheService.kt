package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import java.util.*

class SimulcastCacheService : AbstractCacheService {
    @Inject
    private lateinit var simulcastService: SimulcastService

    fun findAll() = MapCache.getOrCompute(
        "SimulcastCacheService.findAll",
        classes = listOf(Simulcast::class.java, Anime::class.java),
        key = "all"
    ) { AbstractConverter.convert(simulcastService.findAllModified(), SimulcastDto::class.java) }

    fun find(uuid: UUID) = MapCache.getOrCompute(
        "SimulcastCacheService.find",
        classes = listOf(Simulcast::class.java),
        key = uuid
    ) { simulcastService.find(uuid) }

    val currentSimulcast: SimulcastDto?
        get() = findAll()?.firstOrNull()
}