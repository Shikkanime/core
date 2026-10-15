package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.SimulcastEntity
import fr.shikkanime.exposed.repositories.EntityRepository
import fr.shikkanime.models.Season
import kotlin.uuid.Uuid

abstract class SimulcastRepository : EntityRepository<Uuid, SimulcastEntity>(SimulcastEntity) {
    abstract fun findBySeasonAndYear(season: Season, year: Int): SimulcastEntity?
}