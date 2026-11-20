package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.Season
import fr.shikkanime.models.Simulcast

/**
 * Business port of the simulcast airings.
 *
 * @see [Simulcast]
 */
interface SimulcastService {
    /**
     * Finds the simulcast airing of a season and a year, creating it when unknown.
     *
     * @param season season of the airing.
     * @param year year of the airing.
     * @return the stored simulcast, created when it did not exist yet.
     * @see [Simulcast]
     */
    @Transactional
    fun findBySeasonAndYearOrCreate(season: Season, year: Int): Simulcast
}
