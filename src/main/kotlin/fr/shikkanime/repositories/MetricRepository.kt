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

    fun getAverageCpuLoad(from: ZonedDateTime, to: ZonedDateTime): Double? {
        return inTransaction {
            it.createQuery("SELECT AVG(cpuLoad) FROM Metric WHERE date BETWEEN :from AND :to", Double::class.java)
                .setParameter("from", from)
                .setParameter("to", to)
                .singleResult
        }
    }

    fun getAverageMemoryUsage(from: ZonedDateTime, to: ZonedDateTime): Double? {
        return inTransaction {
            it.createQuery("SELECT AVG(memoryUsage) FROM Metric WHERE date BETWEEN :from AND :to", Double::class.java)
                .setParameter("from", from)
                .setParameter("to", to)
                .singleResult
        }
    }
}