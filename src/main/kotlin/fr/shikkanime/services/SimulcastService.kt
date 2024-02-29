package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.repositories.SimulcastRepository
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache

class SimulcastService : AbstractService<Simulcast, SimulcastRepository>() {
    @Inject
    private lateinit var simulcastRepository: SimulcastRepository

    override fun getRepository() = simulcastRepository

    override fun findAll() =
        super.findAll().sortedWith(compareBy({ it.year }, { Constant.seasons.indexOf(it.season) })).reversed()

    fun findBySeasonAndYear(season: String, year: Int) = simulcastRepository.findBySeasonAndYear(season, year)

    override fun save(entity: Simulcast): Simulcast {
        val save = super.save(entity)
        MapCache.invalidate(Simulcast::class.java)
        return save
    }

    override fun delete(entity: Simulcast) {
        super.delete(entity)
        MapCache.invalidate(Simulcast::class.java)
    }
}