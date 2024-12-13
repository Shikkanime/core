package fr.shikkanime.converters.metric

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.utils.withUTCString

class MetricToMetricDtoConverter : AbstractConverter<Metric, MetricDto>() {
    @Converter
    fun convert(from: Metric): MetricDto {
        return MetricDto(
            uuid = from.uuid,
            cpuLoad = (from.cpuLoad * 100).toString().replace(',', '.'),
            memoryUsage = (from.memoryUsage / 1024.0 / 1024.0).toString().replace(',', '.'),
            date = from.date.withUTCString()
        )
    }
}