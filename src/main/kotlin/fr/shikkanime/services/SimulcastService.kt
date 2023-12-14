package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.repositories.SimulcastRepository

class SimulcastService : AbstractService<Simulcast, SimulcastRepository>() {
    @Inject
    private lateinit var simulcastRepository: SimulcastRepository

    override fun getRepository(): SimulcastRepository {
        return simulcastRepository
    }

    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
        return simulcastRepository.findBySeasonAndYear(season, year)
    }
}