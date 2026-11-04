package fr.shikkanime.admin.dtos.anime

import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform
import kotlinx.serialization.Serializable

/**
 * Source link of an anime on a streaming platform, as the administration reads it.
 */
@Serializable
data class AdminAnimePlatformDto(
    val platform: Platform,
    val platformId: String
)

/**
 * Maps a stored anime source link to its administration view.
 *
 * @receiver stored source link.
 * @return the administration view of this source link.
 * @see [AdminAnimePlatformDto]
 */
fun AnimePlatform.toAdminDto(): AdminAnimePlatformDto =
    AdminAnimePlatformDto(
        platform = platform,
        platformId = platformId
    )
