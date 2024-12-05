package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache

class SimulcastCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var simulcastService: SimulcastService

    private val cache = MapCache(
        "SimulcastCacheService.cache",
        classes = listOf(Simulcast::class.java, Anime::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        simulcastService.findAllModified()
    }

    fun findAll() = AbstractConverter.convert(cache[DEFAULT_ALL_KEY], SimulcastDto::class.java)

    val currentSimulcast: SimulcastDto?
        get() = findAll()?.firstOrNull()
}