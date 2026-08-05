package fr.shikkanime.database.services

import fr.shikkanime.database.entities.SimulcastEntity
import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.Season

interface SimulcastService {
    @Transactional
    fun findBySeasonAndYearOrCreate(season: Season, year: Int): SimulcastEntity
}