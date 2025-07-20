package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.GroupedMetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.repositories.MetricRepository
import java.time.ZonedDateTime

class MetricService : AbstractService<Metric, MetricRepository>() {
    @Inject private lateinit var metricRepository: MetricRepository

    override fun getRepository() = metricRepository

    fun findAllAfterGrouped(hours: Long): List<GroupedMetricDto> {
        val date = ZonedDateTime.now().minusHours(hours)
        
        // Determine grouping strategy based on hours
        val groupByMinute = when (hours) {
            1L -> "minute"    // Last hour
            24L -> "hour"     // Last 24 hours: group by hour
            168L -> "day"      // Last week: group by day
            else -> "day" // Default to day for longer periods
        }
        
        return metricRepository.findAllAfterGrouped(date, groupByMinute)
    }

    fun deleteAllBefore(date: ZonedDateTime) = metricRepository.deleteAllBefore(date)
}