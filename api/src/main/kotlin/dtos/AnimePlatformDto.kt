package fr.shikkanime.api.dtos

import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform
import kotlinx.serialization.Serializable

/**
 * API view of a platform source link.
 */
@Serializable
data class AnimePlatformDto(
    val platform: Platform,
    val platformId: String
)

/**
 * Maps a stored platform source link to its API view.
 *
 * @receiver stored platform source link.
 * @return the API view of this source link.
 * @see [AnimePlatformDto]
 */
fun AnimePlatform.toDto(): AnimePlatformDto =
    AnimePlatformDto(
        platform = platform,
        platformId = platformId
    )
