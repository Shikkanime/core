package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.span
import java.util.*

class SimulcastCacheService : AbstractCacheService {
    private val tracer = TelemetryConfig.getTracer("SimulcastCacheService")

    @Inject
    private lateinit var simulcastService: SimulcastService

    fun findAll() = MapCache.getOrCompute(
        "SimulcastCacheService.findAll",
        classes = listOf(Simulcast::class.java, Anime::class.java),
        key = "all"
    ) { tracer.span { simulcastService.findAllModified() } }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "SimulcastCacheService.find",
        classes = listOf(Simulcast::class.java),
        key = uuid
    ) { tracer.span { simulcastService.find(it) } }

    val currentSimulcast: SimulcastDto?
        get() = findAll().firstOrNull()
}