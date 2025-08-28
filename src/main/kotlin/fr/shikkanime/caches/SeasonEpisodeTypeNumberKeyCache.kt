package fr.shikkanime.caches

import fr.shikkanime.entities.enums.EpisodeType
import java.io.Serializable

data class SeasonEpisodeTypeNumberKeyCache(
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SeasonEpisodeTypeNumberKeyCache) return false

        if (season != other.season) return false
        if (number != other.number) return false
        if (episodeType != other.episodeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = season
        result = 31 * result + number
        result = 31 * result + episodeType.hashCode()
        return result
    }

    override fun toString(): String {
        return "$season,$episodeType,$number"
    }
}
