package fr.shikkanime.caches

import fr.shikkanime.entities.enums.EpisodeType
import java.io.Serializable

data class SeasonEpisodeTypeNumberKeyCache(
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
) : Serializable
