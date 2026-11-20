package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Canonical simulcast airing of an anime, identified by its season and year.
 *
 * @see [Season]
 */
data class Simulcast(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val season: Season,
    val year: Int
) : ShikkModel(id, createdAt, updatedAt)
