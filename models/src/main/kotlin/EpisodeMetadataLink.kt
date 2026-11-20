package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Link between an episode and its entry on an external metadata platform.
 *
 * [platformAnimeId] references the external anime the episode belongs to: external catalogs
 * split a franchise into one entry per season, so the episode identifier alone never says
 * which entry it hangs from. The owning episode is referenced by [episodeId], filled by the
 * persistence when the row is attached to its owner, exactly like [Image.entityId].
 *
 * @see [MetadataPlatform]
 * @see [AnimeMetadataLink]
 */
data class EpisodeMetadataLink(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val episodeId: Uuid?,
    val platform: MetadataPlatform,
    val platformId: String,
    val platformAnimeId: String
) : ShikkModel(id, createdAt, updatedAt)
