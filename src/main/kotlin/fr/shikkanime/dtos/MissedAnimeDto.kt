package fr.shikkanime.dtos

data class MissedAnimeDto(
    val anime: AnimeDto,
    val episodeMissed: Long,
)
