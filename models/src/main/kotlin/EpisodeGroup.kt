package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * A group of episode releases published together for one market: the port of
 * the legacy grouped-episode element (`NewGroupIndexer`).
 *
 * All releases of the group share the same `(animeId, episodeType)` bucket and
 * coverage signature — the set of episode coordinates covered by each
 * [LangType]. [coverage] maps every lang type present in the group to the
 * coordinates it covers inside the bucket.
 *
 * @see [EpisodeGrouper]
 * @see [EpisodeObservation]
 */
data class EpisodeGroup(
    val animeId: Uuid,
    val animeSlug: String,
    val episodeType: EpisodeType,
    val releaseDateTime: LocalDateTime,
    val minSeason: Int,
    val maxSeason: Int,
    val minNumber: Int,
    val maxNumber: Int,
    val coverage: Map<LangType, Set<EpisodeCoordinate>>,
    val episodes: List<EpisodeGroupEpisode>,
    val releases: List<EpisodeObservation>
)
