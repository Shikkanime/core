package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Flat release observation consumed by [EpisodeGrouper]: one row per episode
 * release carrying everything needed to bucket and split it, without any
 * persistence or framework type.
 *
 * An episode is identified by its coordinates `(animeId, season, episodeType,
 * number)`; [episodeId] is carried for reference only. Releases are
 * deduplicated by [releaseId].
 *
 * @see [EpisodeGrouper]
 * @see [EpisodeGroup]
 */
data class EpisodeObservation(
    val animeId: Uuid,
    val animeSlug: String,
    val episodeId: Uuid,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int,
    val releaseId: Uuid,
    val audioLocale: Locale,
    val releaseDateTime: LocalDateTime
)
