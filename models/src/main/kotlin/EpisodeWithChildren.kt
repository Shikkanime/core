package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Projection of an [Episode] with every row it owns, composed by the persistence.
 *
 * The domain models are flat and reference their owner by identifier, so an aggregate view is
 * assembled here instead of nesting lists inside [Episode]: one batch read per relation composes a
 * whole page without any lazy walk.
 *
 * The fields of the wrapped [Episode] are exposed as delegates so callers read `animeId` or
 * `season` directly instead of repeating `episode.` on every access, and so method references
 * like `EpisodeWithChildren::animeId` stay usable in collection transforms.
 *
 * @see [Episode]
 */
data class EpisodeWithChildren(
    val episode: Episode,
    val images: List<Image>,
    val localizations: List<EpisodeLocalization>,
    val releases: List<EpisodeRelease>
) {
    val id: Uuid?
        get() = episode.id

    val createdAt: LocalDateTime?
        get() = episode.createdAt

    val updatedAt: LocalDateTime?
        get() = episode.updatedAt

    val animeId: Uuid?
        get() = episode.animeId

    val season: Int
        get() = episode.season

    val episodeType: EpisodeType
        get() = episode.episodeType

    val number: Int
        get() = episode.number

    val releaseDateTime: LocalDateTime
        get() = episode.releaseDateTime

    val duration: Long
        get() = episode.duration
}
