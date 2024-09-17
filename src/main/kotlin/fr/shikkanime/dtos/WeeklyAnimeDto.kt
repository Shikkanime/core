package fr.shikkanime.dtos

import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType

data class WeeklyAnimeDto(
    val anime: AnimeDto,
    val platforms: List<PlatformDto>,
    val releaseDateTime: String,
    val slug: String,
    val langType: LangType,

    val episodeType: EpisodeType? = null,
    val minNumber: Int? = null,
    val maxNumber: Int? = null,
    val number: Int? = null,
    val mappings: List<EpisodeMappingWithoutAnimeDto>? = null
)
