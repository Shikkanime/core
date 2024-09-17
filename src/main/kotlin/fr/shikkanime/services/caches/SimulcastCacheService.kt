package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.withUTCString
import java.time.ZonedDateTime

class SimulcastCacheService : AbstractCacheService {
    @Inject
    private lateinit var simulcastService: SimulcastService

    private val cache = MapCache<String, List<SimulcastDto>>(classes = listOf(Simulcast::class.java)) {
        AbstractConverter.convert(simulcastService.findAll(), SimulcastDto::class.java)!!
    }

    private val modifiedCache = MapCache<String, List<SimulcastDto>>(classes = listOf(Simulcast::class.java)) {
        val list = simulcastService.findAllModified().map { it[0] as Simulcast to it[1] as ZonedDateTime }

        AbstractConverter.convert(list.map { it.first }, SimulcastDto::class.java)!!.onEach { simulcastDto ->
            simulcastDto.lastReleaseDateTime = list.firstOrNull { it.first.uuid == simulcastDto.uuid }!!.second.withUTCString()
        }
    }

    fun findAll() = cache["all"]

    fun findAllModified() = modifiedCache["all"]

    val currentSimulcast: SimulcastDto?
        get() = findAll()?.firstOrNull()
}