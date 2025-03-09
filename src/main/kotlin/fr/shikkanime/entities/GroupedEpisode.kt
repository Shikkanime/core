package fr.shikkanime.entities

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
    val platforms: Set<Platform>,
    val audioLocales: Set<String>,
    val urls: Set<String>,
    val mappings: Set<UUID>,
    var title: String? = null,
    var description: String? = null,
    var duration: Long? = null,
)
