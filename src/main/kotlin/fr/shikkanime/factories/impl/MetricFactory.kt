package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.MetricDto
import fr.shikkanime.entities.Metric
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.utils.withUTCString

class MetricFactory : IGenericFactory<Metric, MetricDto> {
    override fun toDto(entity: Metric) = MetricDto(
        uuid = entity.uuid,
        cpuLoad = (entity.cpuLoad * 100).toString().replace(',', '.'),
        memoryUsage = (entity.memoryUsage / 1024.0 / 1024.0).toString().replace(',', '.'),
        date = entity.date.withUTCString()
    )
}