package fr.shikkanime.caches

import fr.shikkanime.entities.enums.EpisodeType
import java.util.*

data class AnimeUUIDSeasonEpisodeTypeNumberKeyCache(
    val animeUuid: UUID,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
)
