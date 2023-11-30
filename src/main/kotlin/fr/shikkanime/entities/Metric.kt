package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.io.Serializable
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "metric",
    indexes = [
        Index(name = "idx_metrics_date", columnList = "date")
    ]
)
data class Metric(
    @Id
    @UuidGenerator
    val uuid: UUID? = null,
    @Column(name = "cpu_load")
    val cpuLoad: Double = 0.0,
    @Column(name = "memory_usage")
    val memoryUsage: Long = 0,
    @Column(name = "database_size")
    val databaseSize: Long = 0,
    @Column(name = "date")
    val date: ZonedDateTime = ZonedDateTime.now(),
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Metric) return false

        if (uuid != other.uuid) return false
        if (cpuLoad != other.cpuLoad) return false
        if (memoryUsage != other.memoryUsage) return false
        if (databaseSize != other.databaseSize) return false
        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + cpuLoad.hashCode()
        result = 31 * result + memoryUsage.hashCode()
        result = 31 * result + databaseSize.hashCode()
        result = 31 * result + date.hashCode()
        return result
    }

    override fun toString(): String {
        return "Metric(uuid=$uuid, cpuLoad=$cpuLoad, memoryUsage=$memoryUsage, databaseSize=$databaseSize, date=$date)"
    }
}
