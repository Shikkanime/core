package fr.shikkanime.api.dtos

import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * API view of a release of an episode on a streaming platform, in a single audio locale.
 */
@Serializable
data class EpisodeReleaseDto(
    val platform: Platform,
    val platformId: String,
    val url: String,
    val audioLocale: Locale,
    val uncensored: Boolean,
    val releaseDateTime: LocalDateTime
)

/**
 * Maps a stored release to its API view.
 *
 * @receiver stored release.
 * @return the API view of this release.
 * @see [EpisodeReleaseDto]
 */
fun EpisodeRelease.toDto(): EpisodeReleaseDto =
    EpisodeReleaseDto(
        platform = platform,
        platformId = platformId,
        url = url,
        audioLocale = audioLocale,
        uncensored = uncensored,
        releaseDateTime = releaseDateTime
    )
