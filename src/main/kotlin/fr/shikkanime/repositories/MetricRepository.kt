package fr.shikkanime.repositories

import fr.shikkanime.entities.Metric
import org.hibernate.jpa.AvailableHints
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    fun findAllAfter(date: ZonedDateTime): List<Metric> {
        return inTransaction {
            it.createQuery("FROM Metric WHERE date > :date ORDER BY date", Metric::class.java)
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("date", date)
                .resultList
        }
    }

    fun deleteAllBefore(date: ZonedDateTime) {
        inTransaction {
            it.createQuery("DELETE FROM Metric WHERE date < :date")
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("date", date)
                .executeUpdate()
        }
    }
}