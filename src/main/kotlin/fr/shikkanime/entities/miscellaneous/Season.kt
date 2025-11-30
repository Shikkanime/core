package fr.shikkanime.entities.miscellaneous

import java.time.ZonedDateTime

data class Season(
    val number: Int,
    val releaseDateTime: ZonedDateTime,
    val lastReleaseDateTime: ZonedDateTime,
    val episodes: Long
)
