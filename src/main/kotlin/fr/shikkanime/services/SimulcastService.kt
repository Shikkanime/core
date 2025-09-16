package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.Season
import fr.shikkanime.factories.impl.SimulcastFactory
import fr.shikkanime.repositories.SimulcastRepository
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.toTreeSet
import fr.shikkanime.utils.withUTCString
import jakarta.persistence.Tuple
import java.time.ZonedDateTime

class SimulcastService : AbstractService<Simulcast, SimulcastRepository>() {
    @Inject private lateinit var simulcastRepository: SimulcastRepository
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var simulcastFactory: SimulcastFactory

    override fun getRepository() = simulcastRepository

    fun findAllModified() = simulcastRepository.findAllModified()
        .sortedWith(compareBy<Tuple>({ it[0, Simulcast::class.java].year }, { Season.entries.indexOf(it[0, Simulcast::class.java].season) }).reversed())
        .map {
            simulcastFactory.toDto(it[0, Simulcast::class.java]).apply {
                lastReleaseDateTime = it[1, ZonedDateTime::class.java].withUTCString()
                animesCount = it[2, Long::class.java]
            }
        }

    fun findBySeasonAndYear(season: Season, year: Int) = simulcastRepository.findBySeasonAndYear(season, year)

    override fun save(entity: Simulcast): Simulcast {
        val save = super.save(entity)
        traceActionService.createTraceAction(save, TraceAction.Action.CREATE)
        InvalidationService.invalidate(Simulcast::class.java)
        return save
    }

    override fun delete(entity: Simulcast) {
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }

    companion object {
        fun Set<Simulcast>.sortBySeasonAndYear(): Set<Simulcast> =
            this.toTreeSet(compareBy<Simulcast>({ it.year }, { Season.entries.indexOf(it.season) }).reversed())
    }
}