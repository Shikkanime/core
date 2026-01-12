package fr.shikkanime.repositories

import fr.shikkanime.entities.Tag
import fr.shikkanime.entities.Tag_

class TagRepository : AbstractRepository<Tag>() {
    override fun getEntityClass() = Tag::class.java

    override fun findAll(): List<Tag> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.orderBy(cb.asc(cb.lower(root[Tag_.name])))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByName(name: String): Tag? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.equal(cb.lower(root[Tag_.name]), name.lowercase()))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}