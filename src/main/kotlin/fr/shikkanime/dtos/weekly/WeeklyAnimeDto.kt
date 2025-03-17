package fr.shikkanime.dtos.weekly

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType

data class WeeklyAnimeDto(
    val anime: AnimeDto,
    val platforms: Set<PlatformDto>,
    val releaseDateTime: String,
    val slug: String,
    val langTypes: Set<LangType>,

    val episodeType: EpisodeType? = null,
    val minNumber: Int? = null,
    val maxNumber: Int? = null,
    val number: Int? = null,
    val mappings: Set<EpisodeMappingDto>? = null
)
