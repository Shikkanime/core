package fr.shikkanime.models

/**
 * Canonical coordinates of an episode inside its anime: season, editorial
 * type and number. Replaces the legacy `season * 1000 + number` encoding,
 * which collided on large episode numbers and ignored the type.
 *
 * @see [EpisodeGrouper]
 */
data class EpisodeCoordinate(
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
)
