package fr.shikkanime.dtos.weekly

import java.io.Serializable

data class WeeklyAnimesDto(
    val dayOfWeek: String,
    val releases: Set<WeeklyAnimeDto>
) : Serializable
