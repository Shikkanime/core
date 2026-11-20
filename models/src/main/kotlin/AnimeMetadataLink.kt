package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Link between an anime and its entry on an external metadata platform.
 *
 * One anime can carry several links on the same platform: a stored anime aggregating several
 * seasons of a franchise points at one external entry per season, each keeping its own
 * identifier. The owning anime is referenced by [animeId], filled by the persistence when the
 * row is attached to its owner, exactly like [Image.entityId].
 *
 * @see [MetadataPlatform]
 * @see [EpisodeMetadataLink]
 */
data class AnimeMetadataLink(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val animeId: Uuid?,
    val platform: MetadataPlatform,
    val platformId: String
) : ShikkModel(id, createdAt, updatedAt)
