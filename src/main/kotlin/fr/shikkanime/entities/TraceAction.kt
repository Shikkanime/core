package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "trace_action")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class TraceAction(
    uuid: UUID? = null,
    @Column(name = "action_date_time", nullable = false)
    var actionDateTime: ZonedDateTime? = null,
    @Column(name = "entity_type", nullable = false)
    var entityType: String? = null,
    @Column(name = "entity_uuid", nullable = false)
    var entityUuid: UUID? = null,
    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    var action: Action? = null
) : ShikkEntity(uuid) {
    enum class Action {
        CREATE,
        UPDATE,
        DELETE
    }
}