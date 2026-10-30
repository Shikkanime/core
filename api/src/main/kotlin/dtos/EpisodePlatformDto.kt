package fr.shikkanime.api.dtos

import fr.shikkanime.models.EpisodePlatform
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * API view of a platform release of an episode.
 */
@Serializable
data class EpisodePlatformDto(
    val platform: Platform,
    val platformId: String,
    val url: String,
    val audioLocales: Set<Locale>,
    val uncensored: Boolean,
    val releaseDateTime: LocalDateTime
)

/**
 * Maps a stored platform release to its API view.
 *
 * @receiver stored platform release.
 * @return the API view of this release.
 * @see [EpisodePlatformDto]
 */
fun EpisodePlatform.toDto(): EpisodePlatformDto =
    EpisodePlatformDto(
        platform = platform,
        platformId = platformId,
        url = url,
        audioLocales = audioLocales,
        uncensored = uncensored,
        releaseDateTime = releaseDateTime
    )
