package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache

class SimulcastCacheService : AbstractCacheService {
    @Inject
    private lateinit var simulcastService: SimulcastService

    private val cache = MapCache(
        classes = listOf(Simulcast::class.java, Anime::class.java),
        fn = { listOf("all") }
    ) {
        AbstractConverter.convert(simulcastService.findAll(), SimulcastDto::class.java)!!
    }

    private val modifiedCache = MapCache<String, List<SimulcastDto>>(classes = listOf(Simulcast::class.java, Anime::class.java)) {
        AbstractConverter.convert(simulcastService.findAllModified(), SimulcastDto::class.java)!!
    }

    private val seasonYearCache = MapCache<Pair<String, Int>, Simulcast?>(classes = listOf(Simulcast::class.java)) {
        simulcastService.findBySeasonAndYear(it.first, it.second)
    }

    fun findAll() = cache["all"]

    fun findAllModified() = modifiedCache["all"]

    fun findBySeasonAndYear(season: String, year: Int) = seasonYearCache[season to year]

    val currentSimulcast: SimulcastDto?
        get() = findAll()?.firstOrNull()
}