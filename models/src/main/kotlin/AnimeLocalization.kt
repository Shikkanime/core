package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Localized editorial text of an anime for a single language.
 *
 * @property locale Language and region this text applies to.
 * @property title Localized title of the anime.
 * @property description Localized synopsis, `null` when the source does not provide one yet.
 */
data class AnimeLocalization(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val locale: Locale,
    val title: String,
    val description: String?
) : ShikkModel(id, createdAt, updatedAt)
