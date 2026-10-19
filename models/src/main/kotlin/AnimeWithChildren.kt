package fr.shikkanime.models

/**
 * Projection of an [Anime] with every row it owns, composed by the persistence.
 *
 * The domain models are flat and reference their owner by identifier, so an aggregate view is
 * assembled here instead of nesting lists inside [Anime]: one batch read per relation composes a
 * whole page without any lazy walk.
 *
 * @see [Anime]
 */
data class AnimeWithChildren(
    val anime: Anime,
    val images: List<Image>,
    val localizations: List<AnimeLocalization>,
    val platforms: List<AnimePlatform>
)
