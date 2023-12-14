package fr.shikkanime.entities

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "episode")
data class Episode(
    override val uuid: UUID? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var platform: Platform? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var anime: Anime? = null,
    @Column(nullable = false, name = "episode_type")
    @Enumerated(EnumType.STRING)
    val episodeType: EpisodeType? = null,
    @Column(nullable = false, name = "lang_type")
    @Enumerated(EnumType.STRING)
    val langType: LangType? = null,
    @Column(nullable = false, unique = true)
    val hash: String? = null,
    @Column(nullable = false, name = "release_date_time")
    val releaseDateTime: ZonedDateTime = ZonedDateTime.now(),
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
    var duration: Long = -1
) : ShikkEntity(uuid)
