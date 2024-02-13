package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.dtos.simulcasts.UpdatedSimulcastDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache

class SimulcastCacheService : AbstractCacheService() {
    @Inject
    private lateinit var simulcastService: SimulcastService

    private val cache = MapCache<String, List<SimulcastDto>>(classes = listOf(Simulcast::class.java)) {
        AbstractConverter.convert(simulcastService.findAll(), SimulcastDto::class.java)
    }

    private val updatedCache = MapCache<String, List<UpdatedSimulcastDto>>(classes = listOf(Simulcast::class.java)) {
        AbstractConverter.convert(simulcastService.findAll(), UpdatedSimulcastDto::class.java)
    }

    fun findAll() = cache["all"]
    fun findAllUpdated() = updatedCache["all"]
}