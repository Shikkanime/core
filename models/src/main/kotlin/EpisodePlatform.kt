package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Release of an episode on a streaming platform.
 *
 * One row per platform video: [platformId] identifies it on its platform, [audioLocales] lists every
 * audio track the video carries and [releaseDateTime] is the date that platform publishes it. The
 * owning episode is referenced by [episodeId], filled by the persistence when the row is attached to
 * its owner.
 *
 * @see [Episode]
 * @see [Platform]
 */
data class EpisodePlatform(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val episodeId: Uuid?,
    val platform: Platform,
    val platformId: String,
    val url: String,
    val audioLocales: Set<Locale>,
    val uncensored: Boolean,
    val releaseDateTime: LocalDateTime
) : ShikkModel(id, createdAt, updatedAt)
