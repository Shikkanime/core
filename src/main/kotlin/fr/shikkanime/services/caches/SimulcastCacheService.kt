package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.SeasonYearKeyCache
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.Season
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
    ) { simulcastService.findAllModified() }

    fun find(uuid: UUID) = MapCache.getOrComputeNullable(
        "SimulcastCacheService.find",
        classes = listOf(Simulcast::class.java),
        key = uuid
    ) { simulcastService.find(it) }

    fun findBySeasonAndYear(season: Season, year: Int) = MapCache.getOrComputeNullable(
        "SimulcastCacheService.findBySeasonAndYear",
        classes = listOf(Simulcast::class.java),
        key = SeasonYearKeyCache(season, year),
    ) { simulcastService.findBySeasonAndYear(it.season, it.year) }

    val currentSimulcast: SimulcastDto?
        get() = findAll().firstOrNull()
}