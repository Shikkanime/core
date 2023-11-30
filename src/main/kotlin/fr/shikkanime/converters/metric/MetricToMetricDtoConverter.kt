package fr.shikkanime.converters.metric

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.services.MetricService
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MetricToMetricDtoConverter : AbstractConverter<Metric, MetricDto>() {
    private val europeParisZone = ZoneId.of("UTC")
    private val dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ssZ")

    @Inject
    private lateinit var metricService: MetricService

    private fun Double.toDoublePoint() = String.format("%.2f", this)

    override fun convert(from: Metric): MetricDto {
        val minusHours = from.date.minusHours(1)

        return MetricDto(
            uuid = from.uuid,
            cpuLoad = (from.cpuLoad * 100).toString().replace(',', '.'),
            averageCpuLoad = metricService.getAverageCpuLoad(minusHours, from.date)?.times(100)?.toString()?.replace(',', '.') ?: "0",
            memoryUsage = (from.memoryUsage / 1024.0 / 1024.0).toString().replace(',', '.'),
            averageMemoryUsage = metricService.getAverageMemoryUsage(minusHours, from.date)?.div(1024)?.div(1024)?.toString()?.replace(',', '.') ?: "0",
            databaseSize = (from.databaseSize / 1024.0 / 1024.0).toDoublePoint(),
            date = from.date.withZoneSameInstant(europeParisZone).format(dateFormatter)
        )
    }
}