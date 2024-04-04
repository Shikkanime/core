package fr.shikkanime.dtos

import fr.shikkanime.dtos.animes.AnimeDto

data class WeeklyAnimeDto(
    val anime: AnimeDto,
    val releaseDateTime: String,
    val platforms: List<PlatformDto>
)
