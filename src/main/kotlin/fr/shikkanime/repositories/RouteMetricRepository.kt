package fr.shikkanime.repositories

import fr.shikkanime.dtos.RouteMetricDto
import fr.shikkanime.entities.RouteMetric
import fr.shikkanime.entities.RouteMetric_
import java.time.ZonedDateTime

class RouteMetricRepository : AbstractRepository<RouteMetric>() {
    override fun getEntityClass() = RouteMetric::class.java

    fun findAllAggregatedAfter(date: ZonedDateTime): List<RouteMetricDto> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            query.select(
                cb.tuple(
                    root[RouteMetric_.method],
                    root[RouteMetric_.path],
                    cb.avg(root[RouteMetric_.duration]),
                    cb.count(root[RouteMetric_.uuid])
                )
            )

            query.where(cb.greaterThan(root[RouteMetric_.date], date))
            query.groupBy(root[RouteMetric_.method], root[RouteMetric_.path])
            query.orderBy(cb.desc(cb.avg(root[RouteMetric_.duration])))

            createReadOnlyQuery(it, query)
                .resultList
                .map { tuple ->
                    RouteMetricDto(
                        method = tuple[0] as String,
                        path = tuple[1] as String,
                        avgDuration = tuple[2] as Double,
                        count = tuple[3] as Long
                    )
                }
        }
    }

    fun deleteAllBefore(date: ZonedDateTime) {
        database.inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createCriteriaDelete(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.lessThan(root[RouteMetric_.date], date))
            it.createQuery(query).executeUpdate()
        }
    }
}
