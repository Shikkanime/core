package fr.shikkanime.repositories

import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.TraceAction_
import fr.shikkanime.entities.miscellaneous.Pageable
import java.time.LocalDate

class TraceActionRepository : AbstractRepository<TraceAction>() {
    override fun getEntityClass() = TraceAction::class.java

    fun findAllBy(entityType: String?, action: String?, page: Int, limit: Int): Pageable<TraceAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val predicates = mutableListOf(cb.conjunction())
            entityType?.let { entityType -> predicates.add(cb.equal(root[TraceAction_.entityType], entityType)) }
            action?.let { action -> predicates.add(cb.equal(root[TraceAction_.action], action)) }
            query.where(*predicates.toTypedArray())

            query.orderBy(cb.desc(root[TraceAction_.actionDateTime]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun getAnalyticsTraceActions(date: LocalDate): List<TraceAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val actionDate = cb.function("DATE", LocalDate::class.java, root[TraceAction_.actionDateTime])

            query.select(root)
                .where(
                    cb.greaterThan(actionDate, date),
                    cb.equal(root[TraceAction_.entityType], "Member"),
                    cb.equal(root[TraceAction_.action], TraceAction.Action.LOGIN),
                )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}