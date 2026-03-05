package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "route_metric",
    indexes = [
        Index(name = "idx_route_metric_date", columnList = "date")
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class RouteMetric(
    uuid: UUID? = null,
    @Column(nullable = false)
    val method: String = "",
    @Column(nullable = false)
    val path: String = "",
    @Column(nullable = false)
    val duration: Long = 0,
    @Column(nullable = false)
    val date: ZonedDateTime = ZonedDateTime.now(),
) : ShikkEntity(uuid)
