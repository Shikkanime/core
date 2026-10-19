package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import java.time.ZonedDateTime

/**
 * Normalized representation of an episode harvested from a streaming platform.
 *
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
