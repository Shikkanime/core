package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Canonical episode identity of an anime, shared by every platform it is released on.
 *
 * The model is flat on purpose: translatable text lives in [EpisodeLocalization], source releases
 * in [EpisodePlatform] and artwork in [Image], each referencing this row by identifier. An episode
 * is identified by its anime, its season, its type and its number, so a release harvested from two
 * platforms lands on the same row. The aggregated view is a projection composed by the persistence,
 * see [EpisodeWithChildren].
 *
 * @see [EpisodeWithChildren]
 * @see [EpisodePlatform]
 */
data class Episode(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val animeId: Uuid?,
    val season: Int,
    val number: Int,
    val episodeType: EpisodeType,
    val releaseDateTime: LocalDateTime,
    val duration: Long
) : ShikkModel(id, createdAt, updatedAt)
