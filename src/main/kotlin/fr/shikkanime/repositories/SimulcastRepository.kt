package fr.shikkanime.repositories

import fr.shikkanime.entities.Simulcast

class SimulcastRepository : AbstractRepository<Simulcast>() {
    fun findBySeasonAndYear(season: String, year: Int): Simulcast? {
        return inTransaction {
            createReadOnlyQuery(it, "FROM Simulcast WHERE season = :season AND year = :year", Simulcast::class.java)
                .setParameter("season", season)
                .setParameter("year", year)
                .resultList
                .firstOrNull()
        }
    }
}