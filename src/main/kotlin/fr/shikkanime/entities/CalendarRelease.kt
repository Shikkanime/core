package fr.shikkanime.entities

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Entity
@Table(name = "calendar_release")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class CalendarRelease(
    uuid: UUID? = null,
    @Column(nullable = false, name = "release_date")
    val releaseDate: LocalDate = LocalDate.now(),
    @Column(nullable = true, name = "release_time")
    val releaseTime: LocalTime = LocalTime.now(),
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val platform: Platform? = null,
    @Column(nullable = false)
    val anime: String? = null,
    @Column(nullable = false)
    val season: Int? = null,
    @Column(nullable = false, name = "episode_type")
    @Enumerated(EnumType.STRING)
    val episodeType: EpisodeType? = null,
    @Column(nullable = false)
    val number: Int? = null,
    @Column(nullable = false, name = "lang_type")
    @Enumerated(EnumType.STRING)
    val langType: LangType? = null,
) : ShikkEntity(uuid)