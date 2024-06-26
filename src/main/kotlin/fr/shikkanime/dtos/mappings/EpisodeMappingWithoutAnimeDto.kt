package fr.shikkanime.dtos.mappings

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.EpisodeType
import java.util.*

data class EpisodeMappingWithoutAnimeDto(
    val uuid: UUID,
    val releaseDateTime: String,
    val lastReleaseDateTime: String,
    val lastUpdateDateTime: String,
    var episodeType: EpisodeType,
    val season: Int,
    val number: Int,
    val duration: Long,
    val title: String?,
    val description: String?,
    val image: String,
    val status: Status,
)
