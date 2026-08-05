package fr.shikkanime.jobs.platforms

import java.time.ZonedDateTime

data class PlatformEpisode(
    val anime: PlatformAnime,
    val id: String,
    val title: String,
    val description: String?,
    val image: String,
    val releaseDateTime: ZonedDateTime
)
