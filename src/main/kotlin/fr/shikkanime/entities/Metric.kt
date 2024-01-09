package fr.shikkanime.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "metric",
    indexes = [
        Index(name = "idx_metric_date", columnList = "date")
    ]
)
data class Metric(
    override val uuid: UUID? = null,
    @Column(name = "cpu_load")
    val cpuLoad: Double = 0.0,
    @Column(name = "memory_usage")
    val memoryUsage: Long = 0,
    @Column(nullable = false)
    val date: ZonedDateTime = ZonedDateTime.now(),
) : ShikkEntity(uuid)
