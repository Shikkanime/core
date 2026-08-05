package fr.shikkanime.database.services.impl

import fr.shikkanime.database.entities.SimulcastEntity
import fr.shikkanime.database.repositories.SimulcastRepository
import fr.shikkanime.models.Season
import fr.shikkanime.database.services.SimulcastService
import org.koin.core.annotation.Single

@Suppress("unused")
@Single
class SimulcastServiceImpl(
    private val simulcastRepository: SimulcastRepository
) : SimulcastService {
    override fun findBySeasonAndYearOrCreate(season: Season, year: Int): SimulcastEntity =
        simulcastRepository.findBySeasonAndYear(season, year)
            ?: SimulcastEntity.new {
                this.season = season
                this.year = year
            }
}