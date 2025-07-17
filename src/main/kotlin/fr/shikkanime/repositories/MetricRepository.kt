package fr.shikkanime.repositories

import fr.shikkanime.dtos.GroupedMetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.entities.Metric_
import fr.shikkanime.utils.withUTCString
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    override fun getEntityClass() = Metric::class.java

    fun findAllAfterGrouped(date: ZonedDateTime, groupBy: String): List<GroupedMetricDto> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            val groupedDate = cb.function("DATE_TRUNC", ZonedDateTime::class.java, cb.literal(groupBy), root[Metric_.date])

            query.select(
                cb.tuple(
                    groupedDate,
                    cb.avg(root[Metric_.cpuLoad]),
                    cb.avg(root[Metric_.memoryUsage]),
                )
            )

            query.where(cb.greaterThan(root[Metric_.date], date))
            query.groupBy(groupedDate)
            query.orderBy(cb.asc(groupedDate))

            createReadOnlyQuery(it, query)
                .resultList
                .map { tuple ->
                    GroupedMetricDto(
                        date = tuple[0, ZonedDateTime::class.java].withUTCString(),
                        avgCpuLoad = (tuple[1, Double::class.java] * 100).toString().replace(',', '.'),
                        avgMemoryUsage = (tuple[2, Double::class.java] / 1024.0 / 1024.0).toString().replace(',', '.')
                    )
                }
        }
    }

    fun deleteAllBefore(date: ZonedDateTime) {
        database.inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createCriteriaDelete(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.lessThan(root[Metric_.date], date))
            it.createQuery(query).executeUpdate()
        }
    }
}