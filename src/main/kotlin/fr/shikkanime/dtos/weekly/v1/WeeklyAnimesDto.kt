package fr.shikkanime.dtos.weekly.v1

data class WeeklyAnimesDto(
    val dayOfWeek: String,
    val releases: List<WeeklyAnimeDto>
)
