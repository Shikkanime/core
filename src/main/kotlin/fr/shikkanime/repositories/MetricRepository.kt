package fr.shikkanime.repositories

import fr.shikkanime.entities.Metric
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    fun findAllAfter(date: ZonedDateTime): List<Metric> {
        return createQuery("FROM Metric WHERE date > :date ORDER BY date", Metric::class.java)
            .setParameter("date", date)
            .resultList
    }

    fun deleteAllBefore(date: ZonedDateTime) {
        inTransaction {
            it.createQuery("DELETE FROM Metric WHERE date < :date")
                .setParameter("date", date)
                .executeUpdate()
        }
    }
}