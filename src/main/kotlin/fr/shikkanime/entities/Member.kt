package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Role
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "member")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Member(
    override val uuid: UUID? = null,
    @Column(name = "creation_date_time")
    val creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, unique = true)
    val username: String? = null,
    @Column(nullable = false, name = "encrypted_password")
    val encryptedPassword: ByteArray? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "member_roles",
        joinColumns = [JoinColumn(name = "member_uuid")]
    )
    @Enumerated(EnumType.STRING)
    val roles: MutableSet<Role> = mutableSetOf(),
) : ShikkEntity(uuid)