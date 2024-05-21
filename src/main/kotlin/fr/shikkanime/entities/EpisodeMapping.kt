package fr.shikkanime.entities

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.EpisodeType
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "episode_mapping",
    indexes = [
        Index(
            name = "idx_episode_mapping_anime_episode_type_season_number",
            columnList = "anime_uuid, episode_type, season, number",
            unique = true
        ),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class EpisodeMapping(
    uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var anime: Anime? = null,
    @Column(nullable = false, name = "release_date_time")
    var releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false, name = "last_release_date_time")
    var lastReleaseDateTime: ZonedDateTime = releaseDateTime,
    @Column(nullable = false, name = "last_update_date_time")
    var lastUpdateDateTime: ZonedDateTime = releaseDateTime,
    @Column(nullable = false, name = "episode_type")
    @Enumerated(EnumType.STRING)
    var episodeType: EpisodeType? = null,
    @Column(nullable = false)
    var season: Int? = null,
    @Column(nullable = false)
    var number: Int? = null,
    @Column(nullable = false)
    var duration: Long = -1,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var title: String? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var description: String? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var image: String? = null,
    @Column(nullable = true, name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status = Status.VALID,
    @OneToMany(mappedBy = "episode", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var memberFollowEpisodes: MutableSet<MemberFollowEpisode> = mutableSetOf(),
) : ShikkEntity(uuid)
