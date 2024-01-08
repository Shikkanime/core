package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.repositories.SimulcastRepository
import fr.shikkanime.utils.Constant

class SimulcastService : AbstractService<Simulcast, SimulcastRepository>() {
    @Inject
    private lateinit var simulcastRepository: SimulcastRepository

    override fun getRepository() = simulcastRepository

    override fun findAll() = super.findAll().sortedWith(compareBy({ it.year }, { Constant.seasons.indexOf(it.season) }))

    fun findBySeasonAndYear(season: String, year: Int) = simulcastRepository.findBySeasonAndYear(season, year)
}