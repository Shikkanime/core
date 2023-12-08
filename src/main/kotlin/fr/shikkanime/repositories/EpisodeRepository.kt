package fr.shikkanime.repositories

import fr.shikkanime.entities.Episode

class EpisodeRepository : AbstractRepository<Episode>() {
    fun findByHash(hash: String?): Episode? {
        return inTransaction {
            it.createQuery("FROM Episode WHERE LOWER(hash) = :hash", getEntityClass())
                .setParameter("hash", hash?.lowercase())
                .resultList
                .firstOrNull()
        }
    }
}