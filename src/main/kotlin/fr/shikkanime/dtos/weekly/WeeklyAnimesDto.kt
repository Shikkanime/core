package fr.shikkanime.dtos.weekly

data class WeeklyAnimesDto(
    val dayOfWeek: String,
    val releases: Set<WeeklyAnimeDto>
)
