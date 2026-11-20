package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Projection of an [Anime] with every row it owns, composed by the persistence.
 *
 * The domain models are flat and reference their owner by identifier, so an aggregate view is
 * assembled here instead of nesting lists inside [Anime]: one batch read per relation composes a
 * whole page without any lazy walk.
 *
 * The fields of the wrapped [Anime] are exposed as delegates so callers read `id` or
 * `normalizedTitle` directly instead of repeating `anime.` on every access, and so method
 * references like `AnimeWithChildren::normalizedTitle` stay usable in collection transforms.
 *
 * @see [Anime]
 */
data class AnimeWithChildren(
    val anime: Anime,
    val images: List<Image>,
    val localizations: List<AnimeLocalization>,
    val platforms: List<AnimePlatform>
) {
    val id: Uuid?
        get() = anime.id

    val createdAt: LocalDateTime?
        get() = anime.createdAt

    val updatedAt: LocalDateTime?
        get() = anime.updatedAt

    val normalizedTitle: String
        get() = anime.normalizedTitle
}
