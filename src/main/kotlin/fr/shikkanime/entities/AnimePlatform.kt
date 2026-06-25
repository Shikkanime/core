package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.entities.Tracing
import fr.shikkanime.utils.isBeforeOrEqual
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "anime_platform",
    indexes = [
        Index(
            name = "idx_anime_platform_platform_platform_id",
            columnList = "platform, platform_id"
        ),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Tracing
class AnimePlatform(
    uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var anime: Anime? = null,
    @Column(nullable = true, name = "last_update_date_time")
    var lastUpdateDateTime: ZonedDateTime? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val platform: Platform? = null,
    @Column(nullable = false, name = "platform_id")
    var platformId: String? = null,
    @Column(nullable = false)
    override var available: Boolean = true,
) : ShikkEntity(uuid), Availability {
    /**
     * Checks whether the current object is considered invalid based on the provided date and duration.
     *
     * @param zonedDateTime The reference date and time for validation (current time).
     * @param deprecatedDuration The duration in months to determine if the object is outdated.
     *
     * @return `true` if the object's last validation date is before or equal to the computed date
     *         (reference date minus the specified duration), otherwise `false`.
     */
    fun isInvalid(
        zonedDateTime: ZonedDateTime,
        deprecatedDuration: Long
    ): Boolean =
        lastUpdateDateTime?.isBeforeOrEqual(zonedDateTime.minusMonths(deprecatedDuration)) == true
}
