package fr.shikkanime.repositories

import fr.shikkanime.entities.Simulcast
import org.hibernate.jpa.AvailableHints

class SimulcastRepository : AbstractRepository<Simulcast>() {
    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
        return inTransaction {
            it.createQuery("FROM Simulcast WHERE season = :season AND year = :year", Simulcast::class.java)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("season", season)
                .setParameter("year", year)
                .resultList
                .firstOrNull()
        }
    }
}