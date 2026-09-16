package fr.shikkanime.jobs.platforms

import java.time.ZonedDateTime

data class PlatformEpisode(
    val id: String,
    val title: String,
    val releaseDateTime: ZonedDateTime
)
