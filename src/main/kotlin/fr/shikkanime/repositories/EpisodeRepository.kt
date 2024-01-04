package fr.shikkanime.repositories

import fr.shikkanime.entities.Episode
import org.hibernate.Hibernate
import java.util.*

class EpisodeRepository : AbstractRepository<Episode>() {
    private fun Episode.initialize(): Episode {
        Hibernate.initialize(this.anime?.simulcasts)
        return this
    }

    private fun List<Episode>.initialize(): List<Episode> {
        this.forEach { episode -> episode.initialize() }
        return this
    }

    override fun findAll(): List<Episode> {
        return inTransaction {
            it.createQuery("FROM Episode", getEntityClass())
                .resultList
                .initialize()
        }
    }

    fun findAllHashes(): List<String> {
        return inTransaction {
            it.createQuery("SELECT hash FROM Episode", String::class.java)
                .resultList
        }
    }

    fun findByHash(hash: String?): Episode? {
        return inTransaction {
            it.createQuery("FROM Episode WHERE LOWER(hash) LIKE :hash", getEntityClass())
                .setParameter("hash", "%${hash?.lowercase()}%")
                .resultList
                .firstOrNull()
        }
    }

    fun findByAnime(uuid: UUID): List<Episode> {
        return inTransaction {
            it.createQuery("FROM Episode WHERE anime.uuid = :uuid", getEntityClass())
                .setParameter("uuid", uuid)
                .resultList
        }
    }
}