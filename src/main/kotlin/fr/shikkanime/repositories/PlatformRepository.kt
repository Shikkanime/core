package fr.shikkanime.repositories

import fr.shikkanime.entities.Platform

class PlatformRepository : AbstractRepository<Platform>() {
    fun findByName(name: String): Platform? {
        return inTransaction {
            it.createQuery("FROM Platform WHERE name = :name", getEntityClass())
                .setParameter("name", name)
                .resultList
                .firstOrNull()
        }
    }
}