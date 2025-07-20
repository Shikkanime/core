package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "member_follow_anime",
    indexes = [
        Index(name = "member_follow_anime_member_index", columnList = "member_uuid"),
        Index(name = "member_follow_anime_anime_index", columnList = "anime_uuid"),
        Index(name = "member_follow_anime_member_anime_index", columnList = "member_uuid, anime_uuid", unique = true)
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class MemberFollowAnime(
    uuid: UUID? = null,
    @Column(nullable = false, name = "follow_date_time")
    val followDateTime: ZonedDateTime = ZonedDateTime.now(),
    @ManyToOne(optional = false)
    val member: Member? = null,
    @ManyToOne(optional = false)
    var anime: Anime? = null,
) : ShikkEntity(uuid)