package fr.shikkanime.services

import fr.shikkanime.dtos.GroupedMetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.repositories.MetricRepository
import java.time.ZonedDateTime

class MetricService : AbstractService<Metric, MetricRepository>() {
    fun findAllAfterGrouped(hours: Long): List<GroupedMetricDto> {
        val date = ZonedDateTime.now().minusHours(hours)
        
        // Determine grouping strategy based on hours
        val groupByMinute = when (hours) {
            1L -> "minute"    // Last hour
            24L -> "hour"     // Last 24 hours: group by hour
            168L -> "day"      // Last week: group by day
            else -> "day" // Default to day for longer periods
        }

        return repository.findAllAfterGrouped(date, groupByMinute)
    }

    fun deleteAllBefore(date: ZonedDateTime) = repository.deleteAllBefore(date)
}