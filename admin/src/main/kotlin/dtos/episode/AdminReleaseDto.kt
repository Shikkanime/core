package fr.shikkanime.admin.dtos.episode

import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Release of an episode on a streaming platform, as the administration reads it.
 */
@Serializable
data class AdminReleaseDto(
    val platform: Platform,
    val platformId: String,
    val url: String,
    val audioLocale: Locale,
    val uncensored: Boolean,
    val releaseDateTime: LocalDateTime
)

/**
 * Maps a stored release to its administration view.
 *
 * @receiver stored release.
 * @return the administration view of this release.
 * @see [AdminReleaseDto]
 */
fun EpisodeRelease.toAdminDto(): AdminReleaseDto =
    AdminReleaseDto(
        platform = platform,
        platformId = platformId,
        url = url,
        audioLocale = audioLocale,
        uncensored = uncensored,
        releaseDateTime = releaseDateTime
    )
