package fr.shikkanime.entities.miscellaneous

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import java.time.ZonedDateTime
import java.util.*

data class GroupedEpisode(
    val anime: Anime,
    val releaseDateTime: ZonedDateTime,
    val lastUpdateDateTime: ZonedDateTime,
    val minSeason: Int,
    val maxSeason: Int,
    val episodeType: EpisodeType,
    val minNumber: Int,
    val maxNumber: Int,
    val platforms: Array<Platform>,
    val audioLocales: Array<String>,
    val urls: Array<String>,
    val mappings: Array<UUID>,
    var title: String? = null,
    var description: String? = null,
    var duration: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupedEpisode) return false

        return minSeason == other.minSeason &&
                maxSeason == other.maxSeason &&
                minNumber == other.minNumber &&
                maxNumber == other.maxNumber &&
                duration == other.duration &&
                anime == other.anime &&
                releaseDateTime == other.releaseDateTime &&
                lastUpdateDateTime == other.lastUpdateDateTime &&
                episodeType == other.episodeType &&
                platforms.contentEquals(other.platforms) &&
                audioLocales.contentEquals(other.audioLocales) &&
                urls.contentEquals(other.urls) &&
                mappings.contentEquals(other.mappings) &&
                title == other.title &&
                description == other.description
    }

    override fun hashCode(): Int {
        var result = minSeason
        result = 31 * result + maxSeason
        result = 31 * result + minNumber
        result = 31 * result + maxNumber
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + anime.hashCode()
        result = 31 * result + releaseDateTime.hashCode()
        result = 31 * result + lastUpdateDateTime.hashCode()
        result = 31 * result + episodeType.hashCode()
        result = 31 * result + platforms.contentHashCode()
        result = 31 * result + audioLocales.contentHashCode()
        result = 31 * result + urls.contentHashCode()
        result = 31 * result + mappings.contentHashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "GroupedEpisode(anime=$anime, releaseDateTime=$releaseDateTime, lastUpdateDateTime=$lastUpdateDateTime, minSeason=$minSeason, maxSeason=$maxSeason, episodeType=$episodeType, minNumber=$minNumber, maxNumber=$maxNumber, platforms=${platforms.contentToString()}, audioLocales=${audioLocales.contentToString()}, urls=${urls.contentToString()}, mappings=${mappings.contentToString()}, title=$title, description=$description, duration=$duration)"
    }
}