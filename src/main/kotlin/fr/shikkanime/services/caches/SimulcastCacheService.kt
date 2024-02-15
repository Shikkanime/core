package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache

class SimulcastCacheService : AbstractCacheService {
    @Inject
    private lateinit var simulcastService: SimulcastService

    private val cache = MapCache<String, List<SimulcastDto>>(classes = listOf(Simulcast::class.java)) {
        AbstractConverter.convert(simulcastService.findAll(), SimulcastDto::class.java)
    }

    fun findAll() = cache["all"]
}