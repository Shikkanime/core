package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "member_notification_settings",
    indexes = [
        Index(name = "member_notification_settings_member_index", columnList = "member_uuid"),
        Index(name = "member_notification_settings_notification_type_index", columnList = "notification_type"),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class MemberNotificationSettings(
    uuid: UUID? = null,
    @Column(nullable = false, name = "creation_date_time")
    val creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, name = "last_update_date_time")
    var lastUpdateDateTime: ZonedDateTime = ZonedDateTime.now(),
    @OneToOne(optional = false)
    val member: Member? = null,
    @Column(nullable = false, name = "notification_type")
    @Enumerated(EnumType.STRING)
    var notificationType: NotificationType = NotificationType.ALL,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_notification_settings_platforms",
        joinColumns = [JoinColumn(name = "member_notification_settings_uuid")]
    )
    @Enumerated(EnumType.STRING)
    var platforms: MutableSet<Platform> = mutableSetOf(),
) : ShikkEntity(uuid) {
    enum class NotificationType {
        ALL,
        WATCHLIST,
        NONE
    }
}