package fr.shikkanime.models

/**
 * Projection of an [Episode] with every row it owns, composed by the persistence.
 *
 * The domain models are flat and reference their owner by identifier, so an aggregate view is
 * assembled here instead of nesting lists inside [Episode]: one batch read per relation composes a
 * whole page without any lazy walk.
 *
 * @see [Episode]
 */
data class EpisodeWithChildren(
    val episode: Episode,
    val images: List<Image>,
    val localizations: List<EpisodeLocalization>,
    val platforms: List<EpisodePlatform>
)
