package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "member_follow_episode",
    indexes = [
        Index(name = "member_follow_episode_member_index", columnList = "member_uuid"),
        Index(name = "member_follow_episode_episode_index", columnList = "episode_uuid"),
        Index(name = "member_follow_episode_member_episode_index", columnList = "member_uuid, episode_uuid", unique = true)
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class MemberFollowEpisode(
    uuid: UUID? = null,
    @Column(nullable = false, name = "follow_date_time")
    val followDateTime: ZonedDateTime = ZonedDateTime.now(),
    @ManyToOne(optional = false)
    val member: Member? = null,
    @ManyToOne(optional = false)
    val episode: EpisodeMapping? = null,
) : ShikkEntity(uuid)