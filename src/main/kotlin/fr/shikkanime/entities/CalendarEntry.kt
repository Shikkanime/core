package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Platform
import java.time.ZonedDateTime

data class CalendarEntry(
    val anime: Anime,
    val episodeMapping: EpisodeMapping,
    val releaseDateTime: ZonedDateTime,
    val platform: Platform,
    val audioLocale: String,
)
