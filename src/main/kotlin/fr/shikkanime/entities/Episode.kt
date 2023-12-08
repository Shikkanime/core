package fr.shikkanime.entities

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "episode")
data class Episode(
    override val uuid: UUID? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var platform: Platform? = null,
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var anime: Anime? = null,
    @Column(nullable = false, name = "episode_type")
    @Enumerated(EnumType.STRING)
    val episodeType: EpisodeType? = null,
    @Column(nullable = false, name = "lang_type")
    @Enumerated(EnumType.STRING)
    val langType: LangType? = null,
    @Column(nullable = false)
    val hash: String? = null,
    @Column(nullable = false, name = "release_date")
    val releaseDate: ZonedDateTime = ZonedDateTime.now(),
    @Column(nullable = false)
    var season: Int? = null,
    @Column(nullable = false)
    var number: Int? = null,
    @Column(nullable = true, columnDefinition = "TEXT")
    var title: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT")
    var url: String? = null,
    @Column(nullable = false, columnDefinition = "TEXT")
    var image: String? = null,
    @Column(nullable = false)
    var duration: Long = -1
) : ShikkEntity(uuid)
