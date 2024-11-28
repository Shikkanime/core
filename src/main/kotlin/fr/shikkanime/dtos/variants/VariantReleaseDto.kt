package fr.shikkanime.dtos.variants

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.Platform
import java.time.ZonedDateTime

data class VariantReleaseDto(
    val anime: Anime,
    val episodeMapping: EpisodeMapping,
    val releaseDateTime: ZonedDateTime,
    val platform: Platform,
    val audioLocale: String,
)
