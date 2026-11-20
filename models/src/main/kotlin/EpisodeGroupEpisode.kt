package fr.shikkanime.models

import kotlin.uuid.Uuid

/**
 * Episode member of an [EpisodeGroup]: the canonical coordinates plus the
 * identifier of the episode row they resolve to.
 *
 * @see [EpisodeGroup]
 * @see [EpisodeCoordinate]
 */
data class EpisodeGroupEpisode(
    val episodeId: Uuid,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
) {
    fun toCoordinate(): EpisodeCoordinate = EpisodeCoordinate(season, episodeType, number)
}
