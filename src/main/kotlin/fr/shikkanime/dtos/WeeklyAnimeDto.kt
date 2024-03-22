package fr.shikkanime.dtos

data class WeeklyAnimeDto(
    val anime: AnimeDto,
    val releaseDateTime: String,
    val platforms: List<PlatformDto>
)
