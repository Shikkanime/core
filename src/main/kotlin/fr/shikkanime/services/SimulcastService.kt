package fr.shikkanime.services

import com.google.inject.Inject
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

    override fun findAll() =
        super.findAll().sortBySeasonAndYear()

    fun findAllModified() = simulcastRepository.findAllModified()

    fun findBySeasonAndYear(season: String, year: Int) = simulcastRepository.findBySeasonAndYear(season, year)

    override fun save(entity: Simulcast): Simulcast {
        val save = super.save(entity)
        MapCache.invalidate(Simulcast::class.java)
        traceActionService.createTraceAction(save, TraceAction.Action.CREATE)
        return save
    }

    override fun delete(entity: Simulcast) {
        super.delete(entity)
        MapCache.invalidate(Simulcast::class.java)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }

    companion object {
        fun List<Simulcast>.sortBySeasonAndYear(): List<Simulcast> =
            this.sortedWith(compareBy({ it.year }, { Constant.seasons.indexOf(it.season) })).reversed()

        fun Set<Simulcast>.sortBySeasonAndYear(): Set<Simulcast> =
            this.sortedWith(compareBy({ it.year }, { Constant.seasons.indexOf(it.season) })).reversed().toSet()
    }
}