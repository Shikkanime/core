package fr.shikkanime.dtos.animes

import java.io.Serializable

data class MissedAnimeDto(
    val anime: AnimeDto,
    val episodeMissed: Long,
) : Serializable
