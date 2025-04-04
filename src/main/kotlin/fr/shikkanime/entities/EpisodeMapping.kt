package fr.shikkanime.entities

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
        Index(
            name = "idx_episode_mapping_anime_uuid",
            columnList = "anime_uuid"
        ),
        Index(
            name = "idx_episode_mapping_sort_order",
            columnList = "last_release_date_time DESC, season DESC, episode_type DESC, number DESC"
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
    // -----------------------------------------------------------------
    @OneToMany(mappedBy = "mapping", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var variants: MutableSet<EpisodeVariant> = mutableSetOf(),
    @OneToMany(mappedBy = "episode", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var memberFollowEpisodes: MutableSet<MemberFollowEpisode> = mutableSetOf(),
) : ShikkEntity(uuid)
