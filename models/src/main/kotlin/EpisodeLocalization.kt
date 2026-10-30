package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Localized editorial text of an episode for a single language.
 *
 * The owning episode is referenced by [episodeId], filled by the persistence when the row is
 * attached to its owner, exactly like [Image.entityId].
 *
 * @see [Episode]
 */
data class EpisodeLocalization(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val episodeId: Uuid?,
    val locale: Locale,
    val title: String?,
    val description: String?
) : ShikkModel(id, createdAt, updatedAt)
