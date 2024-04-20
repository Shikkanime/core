package fr.shikkanime.entities

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "member_follow_episode")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class MemberFollowEpisode(
    override val uuid: UUID? = null,
    @Column(nullable = false, name = "follow_date_time")
    val followDateTime: ZonedDateTime = ZonedDateTime.now(),
    @ManyToOne(optional = false)
    val member: Member? = null,
    @ManyToOne(optional = false)
    val episode: EpisodeMapping? = null,
) : ShikkEntity(uuid)