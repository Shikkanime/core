package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.EpisodeType
import java.time.ZonedDateTime

/**
 * Normalized representation of an episode harvested from a streaming platform.
 *
 * @property anime Associated metadata for the parent anime show.
 * @property id Identifier of the episode assigned by the source platform.
 * @property title Human-readable title or name of the episode.
 * @property description Optional summary or synopsis of the episode.
 * @property image Direct URL to the high-definition preview image.
 * @property releaseDateTime Publication timestamp provided by the platform.
 * @property season Broadcast season index.
 * @property number Episode sequence index (-1 when unnumbered, such as non-numbered movies).
 * @property episodeType Categorization such as regular broadcast, film, or special.
 * @property duration Video run-time in seconds.
 * @property url Web link to view the episode on the platform.
 * @property audioLocale Canonical audio language code (e.g. ja-JP or fr-FR).
 * @property uncensored Whether the release is an uncensored version.
 * @property original Whether this audio track is the platform's primary/original release.
 */
data class PlatformEpisode(
    val anime: PlatformAnime,
    val id: String,
    val title: String,
    val description: String?,
    val image: String,
    val releaseDateTime: ZonedDateTime,
    val season: Int,
    val number: Int,
    val episodeType: EpisodeType,
    val duration: Long,
    val url: String,
    val audioLocale: String,
    val uncensored: Boolean,
    val original: Boolean
)
