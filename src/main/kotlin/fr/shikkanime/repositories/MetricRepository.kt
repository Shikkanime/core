package fr.shikkanime.repositories

import fr.shikkanime.dtos.GroupedMetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.entities.Metric_
import fr.shikkanime.utils.withUTCString
import java.time.ZonedDateTime

class MetricRepository : AbstractRepository<Metric>() {
    suspend fun findAllAfterGrouped(date: ZonedDateTime, groupBy: String): List<GroupedMetricDto> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(entityClass)
            val groupedDate = cb.function("DATE_TRUNC", ZonedDateTime::class.java, cb.literal(groupBy), root[Metric_.date])

            query.select(
                cb.tuple(
                    groupedDate,
                    cb.avg(root[Metric_.cpuLoad]),
                    cb.avg(root[Metric_.memoryUsage]),
                    cb.avg(root[Metric_.threadCount]),
                    cb.max(root[Metric_.peakThreadCount]),
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
                        avgMemoryUsage = (tuple[2, Double::class.java] / 1024.0 / 1024.0).toString().replace(',', '.'),
                        avgThreadCount = tuple[3, Double::class.java].toString().replace(',', '.'),
                        maxPeakThreadCount = tuple[4, Int::class.java].toString()
                    )
                }
        }
    }

    suspend fun deleteAllBefore(date: ZonedDateTime) {
        dispatch(true) {
            val cb = it.criteriaBuilder
            val query = cb.createCriteriaDelete(entityClass)
            val root = query.from(entityClass)
            query.where(cb.lessThan(root[Metric_.date], date))
            it.createQuery(query).executeUpdate()
        }
    }
}