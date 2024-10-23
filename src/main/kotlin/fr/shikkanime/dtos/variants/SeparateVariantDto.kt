package fr.shikkanime.dtos.variants

import fr.shikkanime.entities.enums.EpisodeType

data class SeparateVariantDto(
    val episodeType: EpisodeType,
    val season: Int,
    val number: Int,
)
