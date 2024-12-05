package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.simulcasts.SimulcastModifiedDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.SimulcastRepository
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache

class SimulcastService : AbstractService<Simulcast, SimulcastRepository>() {
    @Inject
    private lateinit var simulcastRepository: SimulcastRepository

    @Inject
    private lateinit var traceActionService: TraceActionService

    override fun getRepository() = simulcastRepository

    fun findAllModified() = simulcastRepository.findAllModified().sortBySeasonAndYear()

    fun findBySeasonAndYear(season: String, year: Int) = simulcastRepository.findBySeasonAndYear(season, year)

    override fun save(entity: Simulcast): Simulcast {
        val save = super.save(entity)
        traceActionService.createTraceAction(save, TraceAction.Action.CREATE)
        return save
    }

    override fun delete(entity: Simulcast) {
        super.delete(entity)
        MapCache.invalidate(Simulcast::class.java)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }

    companion object {
        fun Set<Simulcast>.sortBySeasonAndYear(): Set<Simulcast> =
            this.sortedWith(compareBy({ it.year }, { Constant.seasons.indexOf(it.season) })).reversed().toSet()

        fun List<SimulcastModifiedDto>.sortBySeasonAndYear(): List<SimulcastModifiedDto> =
            this.sortedWith(compareBy({ it.simulcast.year }, { Constant.seasons.indexOf(it.simulcast.season) })).reversed()
    }
}