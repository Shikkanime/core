package fr.shikkanime.repositories

import fr.shikkanime.entities.Episode

class EpisodeRepository : AbstractRepository<Episode>() {
    fun findAllHashes(): List<String> {
        return inTransaction {
            it.createQuery("SELECT hash FROM Episode", String::class.java)
                .resultList
        }
    }

    fun findByHash(hash: String?): Episode? {
        return inTransaction {
            it.createQuery("FROM Episode WHERE LOWER(hash) = :hash", getEntityClass())
                .setParameter("hash", hash?.lowercase())
                .resultList
                .firstOrNull()
        }
    }
}