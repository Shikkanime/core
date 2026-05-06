package fr.shikkanime.repositories

import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.Rule_

class RuleRepository : AbstractRepository<Rule>() {
    override suspend fun findAll(): List<Rule> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.orderBy(cb.asc(root[Rule_.creationDateTime]))
            createReadOnlyQuery(it, query).resultList
        }
    }
}