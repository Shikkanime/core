package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "rule")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Rule(
    uuid: UUID? = null,
    @Column(name = "creation_date_time", nullable = false)
    var creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "platform", nullable = false)
    @Enumerated(EnumType.STRING)
    var platform: Platform? = null,
    @Column(name = "series_id", nullable = false)
    var seriesId: String? = null,
    @Column(name = "season_id", nullable = false)
    var seasonId: String? = null,
    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    var action: Action? = null,
    @Column(name = "action_value", nullable = false)
    var actionValue: String? = null,
    @Column(name = "last_usage_date_time", nullable = true)
    var lastUsageDateTime: ZonedDateTime? = null,
) : ShikkEntity(uuid) {
    enum class Action {
        REPLACE_ANIME_NAME,
        REPLACE_SEASON_NUMBER,
        ADD_TO_NUMBER
    }
}