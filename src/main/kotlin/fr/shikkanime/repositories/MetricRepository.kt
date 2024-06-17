package fr.shikkanime.repositories

import fr.shikkanime.entities.Metric
import fr.shikkanime.entities.Metric_
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    override fun getEntityClass() = Metric::class.java

    fun findAllAfter(date: ZonedDateTime): List<Metric> {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.where(cb.greaterThan(root[Metric_.date], date))
        query.orderBy(cb.asc(root[Metric_.date]))

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
    }

    fun deleteAllBefore(date: ZonedDateTime) {
        inTransaction {
            database.entityManager.createQuery("DELETE FROM Metric WHERE date < :date")
                .setParameter("date", date)
                .executeUpdate()
        }
    }
}