package fr.shikkanime.dtos

import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.enums.LangType

data class WeeklyAnimeDto(
    val anime: AnimeDto,
    val releaseDateTime: String,
    val langType: LangType,
    val platforms: List<PlatformDto>,
    val variant: EpisodeVariantWithoutMappingDto?
)
