package fr.shikkanime.dtos

import fr.shikkanime.dtos.animes.DetailedAnimeDto

data class MissedAnimeDto(
    val anime: DetailedAnimeDto,
    val episodeMissed: Long,
)
