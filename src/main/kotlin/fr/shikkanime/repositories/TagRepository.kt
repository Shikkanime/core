package fr.shikkanime.repositories

import fr.shikkanime.entities.Tag_
import fr.shikkanime.entities.Tag

class TagRepository : AbstractRepository<Tag>() {
    override fun getEntityClass() = Tag::class.java

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