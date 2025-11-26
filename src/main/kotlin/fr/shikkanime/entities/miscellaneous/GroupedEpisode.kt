package fr.shikkanime.entities.miscellaneous

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.EpisodeType
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
    val mappings: Set<UUID>,
    val variants: Collection<EpisodeVariant>,
    var title: String? = null,
    var description: String? = null,
    var duration: Long? = null,
)