package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import java.time.ZonedDateTime

/**
 * Normalized representation of an episode harvested from a streaming platform.
 *
 * @property anime Associated metadata for the parent anime show.
 * @property id Identifier of the episode assigned by the source platform.
 * @property image Direct URL to the high-definition preview image.
 * @property releaseDateTime Publication timestamp provided by the platform.
 * @property season Broadcast season index.
 * @property number Episode sequence index (-1 when unnumbered, such as non-numbered movies).
 * @property episodeType Categorization such as regular broadcast, film, or special.
 * @property duration Video run-time in seconds.
 * @property url Web link to view the episode on the platform.
 * @property audioLocales Canonical audio language codes available for this release.
 * @property uncensored Whether the release is an uncensored version.
 * @property localizations Localized textual representations (title, description) for the release.
 */
data class PlatformEpisode(
    val anime: PlatformAnime,
    val id: String,
    val image: String,
    val releaseDateTime: ZonedDateTime,
    val season: Int,
    val number: Int,
    val episodeType: EpisodeType,
    val duration: Long,
    val url: String,
    val audioLocales: List<Locale>,
    val uncensored: Boolean,
    val localizations: List<PlatformEpisodeLocalization>
)
