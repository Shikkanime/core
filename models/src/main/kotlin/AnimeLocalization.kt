package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Localized editorial text of an anime for a single language.
 *
 * The owning anime is referenced by [animeId], filled by the persistence when the row is attached
 * to its owner, exactly like [Image.entityId].
 */
data class AnimeLocalization(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val animeId: Uuid?,
    val locale: Locale,
    val title: String,
    val description: String?
) : ShikkModel(id, createdAt, updatedAt)
