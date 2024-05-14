package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "metric",
    indexes = [
        Index(name = "idx_metric_date", columnList = "date")
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Metric(
    uuid: UUID? = null,
    @Column(name = "cpu_load")
    val cpuLoad: Double = 0.0,
    @Column(name = "memory_usage")
    val memoryUsage: Long = 0,
    @Column(nullable = false)
    val date: ZonedDateTime = ZonedDateTime.now(),
) : ShikkEntity(uuid)
