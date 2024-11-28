package fr.shikkanime.dtos.animes

import java.time.ZonedDateTime
import java.util.UUID

data class AnimeAudioLocalesSeasonsDto(
    val animeUuid: UUID,
    val audioLocale: String,
    val season: Int,
    val lastReleaseDateTime: ZonedDateTime,
)