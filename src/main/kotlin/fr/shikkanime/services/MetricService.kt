package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.caches.FromToZonedDateTimeKeyCache
import fr.shikkanime.entities.Metric
import fr.shikkanime.repositories.MetricRepository
import fr.shikkanime.utils.MapCache
import java.time.Duration
import java.time.ZonedDateTime

class MetricService : AbstractService<Metric, MetricRepository>() {
    @Inject
    private lateinit var metricRepository: MetricRepository

    private val averageCpuLoadCache = MapCache<FromToZonedDateTimeKeyCache, Double>(Duration.ofHours(1)) {
        metricRepository.getAverageCpuLoad(it.from, it.to)!!
    }

    private val averageMemoryUsageCache = MapCache<FromToZonedDateTimeKeyCache, Double>(Duration.ofHours(1)) {
        metricRepository.getAverageMemoryUsage(it.from, it.to)!!
    }

    override fun getRepository(): MetricRepository {
        return metricRepository
    }

    fun findAllAfter(date: ZonedDateTime): List<Metric> {
        return metricRepository.findAllAfter(date)
    }

    fun getAverageCpuLoad(from: ZonedDateTime, to: ZonedDateTime): Double {
        return averageCpuLoadCache[FromToZonedDateTimeKeyCache(from, to)]
    }

    fun getAverageMemoryUsage(from: ZonedDateTime, to: ZonedDateTime): Double {
        return averageMemoryUsageCache[FromToZonedDateTimeKeyCache(from, to)]
    }

    fun getSize(): Long {
        return metricRepository.getSize()
    }
}