package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime

class AnimeRepository : AbstractRepository<Anime>() {
    fun findByName(name: String?): Anime? {
        return inTransaction {
            it.createQuery("FROM Anime WHERE LOWER(name) = :name", getEntityClass())
                .setParameter("name", name?.lowercase())
                .resultList
                .firstOrNull()
        }
    }
}