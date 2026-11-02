package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Release of an episode on a streaming platform, in a single audio locale.
 *
 * One row per platform video and audio track: [platformId] identifies the video on its platform,
 * [audioLocale] is the only audio track this row plays, and [releaseDateTime] is the date that
 * platform publishes it. A video carrying several audio tracks is stored as one row per track: the
 * identifier, the date and the URL of a dubbed release differ from those of a subtitled one, and a
 * viewer follows one locale. The owning episode is referenced by [episodeId], filled by the
 * persistence when the row is attached to its owner.
 *
 * @see [Episode]
 * @see [Platform]
 */
data class EpisodeRelease(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val episodeId: Uuid?,
    val platform: Platform,
    val platformId: String,
    val url: String,
    val audioLocale: Locale,
    val uncensored: Boolean,
    val releaseDateTime: LocalDateTime
) : ShikkModel(id, createdAt, updatedAt)
