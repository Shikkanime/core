package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Metric
import fr.shikkanime.repositories.MetricRepository
import java.time.ZonedDateTime

class MetricService : AbstractService<Metric, MetricRepository>() {
    @Inject
    private lateinit var metricRepository: MetricRepository

    override fun getRepository() = metricRepository

    fun findAllAfter(date: ZonedDateTime) = metricRepository.findAllAfter(date)

    fun deleteAllBefore(date: ZonedDateTime) = metricRepository.deleteAllBefore(date)
}