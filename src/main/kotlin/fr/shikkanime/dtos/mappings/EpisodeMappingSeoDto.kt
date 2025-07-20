package fr.shikkanime.dtos.mappings

import fr.shikkanime.entities.enums.EpisodeType
import java.time.ZonedDateTime

data class EpisodeMappingSeoDto(
    val animeSlug: String,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int,
    val lastReleaseDateTime: ZonedDateTime
)
