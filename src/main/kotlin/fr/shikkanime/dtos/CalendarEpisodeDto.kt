package fr.shikkanime.dtos

data class CalendarEpisodeDto(
    val anime: String,
    val season: Int = 1,
    val episode: String,
    val platform: String
)
