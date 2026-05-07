package fr.shikkanime.repositories

import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.Rule_

class RuleRepository : AbstractRepository<Rule>() {
    override suspend fun findAll(): List<Rule> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)
            query.orderBy(cb.asc(root[Rule_.creationDateTime]))
            createReadOnlyQuery(it, query).resultList
        }
    }
}