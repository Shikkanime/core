package fr.shikkanime.repositories

import fr.shikkanime.entities.Metric
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    fun findAllAfter(date: ZonedDateTime): List<Metric> {
        return inTransaction {
            it.createQuery("FROM Metric WHERE date > :date", getEntityClass())
                .setParameter("date", date)
                .resultList
        }
    }
}