package fr.shikkanime.entities.miscellaneous

import fr.shikkanime.entities.Anime

data class MissedAnime(
    val anime: Anime,
    val episodeMissed: Long,
)