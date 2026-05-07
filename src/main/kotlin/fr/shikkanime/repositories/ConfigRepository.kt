package fr.shikkanime.repositories

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Config_

class ConfigRepository : AbstractRepository<Config>() {
    suspend fun findAllByName(name: String): List<Config> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)
            query.where(cb.like(cb.lower(root[Config_.propertyKey]), "%${name.lowercase()}%"))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findByName(name: String): Config? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)
            query.where(cb.equal(cb.lower(root[Config_.propertyKey]), name.lowercase()))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}