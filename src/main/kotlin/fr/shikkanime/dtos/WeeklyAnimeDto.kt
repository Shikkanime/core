package fr.shikkanime.dtos

import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class WeeklyAnimeDto(
    val anime: AnimeDto,
    val platforms: List<PlatformDto>,
    val releaseDateTime: String,
    val slug: String,
    val langType: LangType,
    val isReleased: Boolean,
    val isMultipleReleased: Boolean,
    val mappings: List<UUID>,
    val episodeType: EpisodeType? = null,
    val minNumber: Int? = null,
    val maxNumber: Int? = null,
    val number: Int? = null,
)
