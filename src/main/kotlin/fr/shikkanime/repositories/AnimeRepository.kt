package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode

class AnimeRepository : AbstractRepository<Anime>() {
    fun findByName(countryCode: CountryCode, name: String?): Anime? {
        return inTransaction {
            it.createQuery("FROM Anime WHERE countryCode = :countryCode AND LOWER(name) = :name", getEntityClass())
                .setParameter("countryCode", countryCode)
                .setParameter("name", name?.lowercase())
                .resultList
                .firstOrNull()
        }
    }
}