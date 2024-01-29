package fr.shikkanime.repositories

import fr.shikkanime.entities.Config
import org.hibernate.jpa.AvailableHints

class ConfigRepository : AbstractRepository<Config>() {
    fun findAllByName(name: String): List<Config> {
        return inTransaction {
            it.createQuery("FROM Config c WHERE LOWER(c.propertyKey) LIKE :name", getEntityClass())
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("name", "%${name.lowercase()}%")
                .resultList
        }
    }

    fun findByName(name: String): Config? {
        return inTransaction {
            it.createQuery("FROM Config c WHERE c.propertyKey = :name", getEntityClass())
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("name", name)
                .resultList
                .firstOrNull()
        }
    }
}