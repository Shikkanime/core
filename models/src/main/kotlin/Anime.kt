package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Canonical anime identity, shared by every country and language.
 *
 * The model is flat on purpose: translatable text lives in [AnimeLocalization], source links in
 * [AnimePlatform] and artwork in [Image], each referencing this row by identifier. The aggregated
 * view is a projection composed by the persistence, see [AnimeWithChildren].
 *
 * @see [AnimeWithChildren]
 */
data class Anime(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val normalizedTitle: String
) : ShikkModel(id, createdAt, updatedAt)
