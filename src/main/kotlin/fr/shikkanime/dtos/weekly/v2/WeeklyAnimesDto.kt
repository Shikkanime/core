package fr.shikkanime.dtos.weekly.v2

data class WeeklyAnimesDto(
    val dayOfWeek: String,
    val releases: Set<WeeklyAnimeDto>
)
