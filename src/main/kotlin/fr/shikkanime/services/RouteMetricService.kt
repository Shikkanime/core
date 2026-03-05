package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.RouteMetric
import fr.shikkanime.repositories.RouteMetricRepository
import java.time.ZonedDateTime

class RouteMetricService : AbstractService<RouteMetric, RouteMetricRepository>() {
    @Inject private lateinit var routeMetricRepository: RouteMetricRepository

    override fun getRepository() = routeMetricRepository

    fun deleteAllBefore(date: ZonedDateTime) = routeMetricRepository.deleteAllBefore(date)
}
