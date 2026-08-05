package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.SimulcastEntity
import fr.shikkanime.database.entities.SimulcastTable
import fr.shikkanime.database.repositories.SimulcastRepository
import fr.shikkanime.models.Season
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [SimulcastRepository::class])
class SimulcastRepositoryImpl : SimulcastRepository() {
    override fun findBySeasonAndYear(season: Season, year: Int): SimulcastEntity? =
        SimulcastEntity.find {
            (SimulcastTable.season eq season) and
                    (SimulcastTable.year eq year)
        }
            .limit(1)
            .firstOrNull()
}