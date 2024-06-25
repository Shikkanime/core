package fr.shikkanime.repositories

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Config_

class ConfigRepository : AbstractRepository<Config>() {
    override fun getEntityClass() = Config::class.java

    fun findAllByName(name: String): List<Config> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.where(cb.like(cb.lower(root[Config_.propertyKey]), "%${name.lowercase()}%"))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findByName(name: String): Config? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.where(cb.equal(cb.lower(root[Config_.propertyKey]), name.lowercase()))

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }
}