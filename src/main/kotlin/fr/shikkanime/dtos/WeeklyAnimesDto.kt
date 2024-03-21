package fr.shikkanime.dtos

data class WeeklyAnimesDto(
    val dayOfWeek: String,
    val releases: List<WeeklyAnimeDto>
)
