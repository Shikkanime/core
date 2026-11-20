package fr.shikkanime.models

import kotlin.uuid.Uuid

/**
 * An [EpisodeGroup] hydrated with the stored [EpisodeRelease] rows its
 * observations resolve to.
 *
 * [EpisodeGrouper] only needs the flat [EpisodeObservation] fields, so platform
 * details (platform, url, uncensored) are fetched separately and keyed by
 * release identifier here: read APIs expose them without a lookup per group.
 *
 * @see [EpisodeGroup]
 * @see [EpisodeRelease]
 */
data class EpisodeGroupDetail(
    val group: EpisodeGroup,
    val releases: Map<Uuid, EpisodeRelease>
)
