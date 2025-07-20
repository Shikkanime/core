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
    uuid: UUID? = null,
    @Column(nullable = false, name = "creation_date_time")
    val creationDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = true, name = "last_update_date_time")
    var lastUpdateDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, name = "is_private")
    val isPrivate: Boolean = false,
    @Column(nullable = false, unique = true)
    var username: String? = null,
    @Column(nullable = true, unique = true)
    var email: String? = null,
    @Column(nullable = false, name = "encrypted_password")
    val encryptedPassword: ByteArray? = null,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_roles",
        joinColumns = [JoinColumn(name = "member_uuid")]
    )
    @Enumerated(EnumType.STRING)
    val roles: MutableSet<Role> = mutableSetOf(),
    // -----------------------------------------------------------------
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var followedAnimes: MutableSet<MemberFollowAnime> = mutableSetOf(),
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_uuid")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var attachments: MutableSet<Attachment> = mutableSetOf(),
) : ShikkEntity(uuid)