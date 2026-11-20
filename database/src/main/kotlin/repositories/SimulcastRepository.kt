package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.SimulcastEntity
import fr.shikkanime.models.Season
import fr.shikkanime.database.services.SimulcastService
import fr.shikkanime.models.Simulcast
import kotlin.uuid.Uuid

/**
 * Persistence port of the simulcast airings, read as [Simulcast] domain models.
 *
 * @see [SimulcastService]
 */
internal abstract class SimulcastRepository :
    ModelRepository<Uuid, SimulcastEntity, Simulcast>(SimulcastEntity) {
    /**
     * Finds the simulcast airing of a season and a year.
     *
     * @param season season of the airing.
     * @param year year of the airing.
     * @return the matching simulcast, or `null` when it does not exist yet.
     * @see [Simulcast]
     */
    abstract fun findBySeasonAndYear(season: Season, year: Int): Simulcast?

    /**
     * Persists a new simulcast airing.
     *
     * @param simulcast simulcast to persist.
     * @return the simulcast as stored in database.
     * @see [Simulcast]
     */
    abstract fun save(simulcast: Simulcast): Simulcast
}
