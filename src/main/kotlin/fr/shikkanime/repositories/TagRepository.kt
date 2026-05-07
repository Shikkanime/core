package fr.shikkanime.repositories

import fr.shikkanime.entities.Tag
import fr.shikkanime.entities.Tag_

class TagRepository : AbstractRepository<Tag>() {
    suspend fun findByName(name: String): Tag? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)
            query.where(cb.equal(cb.lower(root[Tag_.name]), name.lowercase()))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}