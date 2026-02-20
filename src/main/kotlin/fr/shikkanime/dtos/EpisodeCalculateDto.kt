package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.EpisodeType
import java.time.ZonedDateTime
import java.util.UUID

data class EpisodeCalculateDto(
    val animeUuid: UUID,
    val releaseDateTime: ZonedDateTime,
    val episodeType: EpisodeType,
    val number: Int,
    var previousReleaseDateTime: ZonedDateTime? = null,
)
