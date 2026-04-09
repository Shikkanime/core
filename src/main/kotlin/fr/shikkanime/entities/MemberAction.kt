package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Action
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "member_action")
class MemberAction(
    uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var member: Member? = null,
    @Column(nullable = false, name = "creation_date_time")
    var creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, name = "update_date_time")
    var updateDateTime: ZonedDateTime = creationDateTime,
    @Column(nullable = false)
    var email: String? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var action: Action? = null,
    @Column(nullable = false)
    var validated: Boolean = false,
    @Column(nullable = false)
    var code: String? = null,
) : ShikkEntity(uuid)