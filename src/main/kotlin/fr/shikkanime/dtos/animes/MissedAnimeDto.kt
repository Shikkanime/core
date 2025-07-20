package fr.shikkanime.dtos.animes

data class MissedAnimeDto(
    val anime: AnimeDto,
    val episodeMissed: Long,
)
