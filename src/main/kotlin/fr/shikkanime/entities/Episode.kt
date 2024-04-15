package fr.shikkanime.entities

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "episode",
    indexes = [
        Index(name = "idx_episode_release_date_time", columnList = "release_date_time"),
        Index(name = "idx_episode_season", columnList = "season"),
        Index(name = "idx_episode_number", columnList = "number"),
        Index(name = "idx_episode_episode_type", columnList = "episode_type"),
        Index(name = "idx_episode_lang_type", columnList = "lang_type"),
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Deprecated("Use EpisodeMapping instead")
class Episode(
    override val uuid: UUID? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var platform: Platform? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var anime: Anime? = null,
    @Column(nullable = false, name = "episode_type")
    @Enumerated(EnumType.STRING)
    var episodeType: EpisodeType? = null,
    @Column(nullable = false, name = "lang_type")
    @Enumerated(EnumType.STRING)
    var langType: LangType? = null,
    @Column(nullable = true, name = "audio_locale")
    var audioLocale: String? = null,
    @Column(nullable = false, unique = true)
    var hash: String? = null,
    @Column(nullable = false, name = "release_date_time")
    var releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false)
    var season: Int? = null,
    @Column(nullable = false)
    var number: Int? = null,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var title: String? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var url: String? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(1000)")
    var image: String? = null,
    @Column(nullable = false)
    var duration: Long = -1,
    @Column(nullable = true, columnDefinition = "VARCHAR(1000)")
    var description: String? = null,
    @Column(nullable = true, name = "last_update_date_time")
    var lastUpdateDateTime: ZonedDateTime? = releaseDateTime,
    @Column(nullable = true, name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status = Status.VALID,
) : ShikkEntity(uuid)
