package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.SimulcastRepository
import fr.shikkanime.database.services.SimulcastService
import fr.shikkanime.models.Season
import fr.shikkanime.models.Simulcast
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [SimulcastService::class])
internal class SimulcastServiceImpl(
    private val simulcastRepository: SimulcastRepository
) : SimulcastService {
    override fun findBySeasonAndYearOrCreate(season: Season, year: Int): Simulcast =
        // Resolve the airing first: a known season-year pair is reused as is
        simulcastRepository.findBySeasonAndYear(season, year)
            // Persist a new airing only when the pair is unknown
            ?: simulcastRepository.save(
                Simulcast(
                    id = null,
                    createdAt = null,
                    updatedAt = null,
                    season = season,
                    year = year
                )
            )
}
