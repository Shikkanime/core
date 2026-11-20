package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.SimulcastEntity
import fr.shikkanime.database.entities.SimulcastTable
import fr.shikkanime.database.repositories.SimulcastRepository
import fr.shikkanime.models.Season
import fr.shikkanime.models.Simulcast
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [SimulcastRepository::class])
internal class SimulcastRepositoryImpl : SimulcastRepository() {
    override fun findBySeasonAndYear(season: Season, year: Int): Simulcast? =
        SimulcastEntity.find {
            (SimulcastTable.season eq season) and
                (SimulcastTable.year eq year)
        }
            .limit(1)
            .firstOrNull()
            ?.toModel()

    override fun save(simulcast: Simulcast): Simulcast =
        SimulcastEntity.new {
            this.season = simulcast.season
            this.year = simulcast.year
        }
            .applyFlush()
            .toModel()
}
