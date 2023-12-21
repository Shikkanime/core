package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Role
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "member")
class Member(
    override val uuid: UUID? = null,
    @Column(name = "creation_date_time")
    val creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, unique = true)
    val username: String? = null,
    @Column(nullable = false, name = "encrypted_password")
    val encryptedPassword: ByteArray? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val role: Role = Role.GUEST,
) : ShikkEntity(uuid)