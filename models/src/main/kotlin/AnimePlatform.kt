package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Source link between an anime and its identifier on a streaming platform.
 *
 * The owning anime is referenced by [animeId], filled by the persistence when the row is attached
 * to its owner, exactly like [Image.entityId].
 */
data class AnimePlatform(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val animeId: Uuid?,
    val platform: Platform,
    val platformId: String
) : ShikkModel(id, createdAt, updatedAt)
