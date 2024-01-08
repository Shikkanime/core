package fr.shikkanime.repositories

import fr.shikkanime.entities.Config

class ConfigRepository : AbstractRepository<Config>() {
    fun findAllByName(name: String): List<Config> {
        return inTransaction {
            it.createQuery("FROM Config c WHERE LOWER(c.propertyKey) LIKE :name", getEntityClass())
                .setParameter("name", "%${name.lowercase()}%")
                .resultList
        }
    }

    fun findByName(name: String): Config? {
        return inTransaction {
            it.createQuery("FROM Config c WHERE c.propertyKey = :name", getEntityClass())
                .setParameter("name", name)
                .resultList
                .firstOrNull()
        }
    }
}