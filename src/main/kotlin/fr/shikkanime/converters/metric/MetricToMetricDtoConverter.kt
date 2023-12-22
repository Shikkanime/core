package fr.shikkanime.converters.metric

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.entities.Metric
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MetricToMetricDtoConverter : AbstractConverter<Metric, MetricDto>() {
    private val utcZone = ZoneId.of("UTC")
    private val dateFormatter = DateTimeFormatter.ofPattern("HH:mm:ssZ")

    override fun convert(from: Metric): MetricDto {
        return MetricDto(
            uuid = from.uuid,
            cpuLoad = (from.cpuLoad * 100).toString().replace(',', '.'),
            memoryUsage = (from.memoryUsage / 1024.0 / 1024.0).toString().replace(',', '.'),
            date = from.date.withZoneSameInstant(utcZone).format(dateFormatter)
        )
    }
}