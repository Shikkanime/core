package fr.shikkanime.repositories

import fr.shikkanime.entities.Metric
import fr.shikkanime.entities.Metric_
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    override fun getEntityClass() = Metric::class.java

    fun findAllAfter(date: ZonedDateTime): List<Metric> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.greaterThan(root[Metric_.date], date))
            query.orderBy(cb.asc(root[Metric_.date]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun deleteAllBefore(date: ZonedDateTime) {
        inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createCriteriaDelete(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.lessThan(root[Metric_.date], date))
            it.createQuery(query).executeUpdate()
        }
    }
}