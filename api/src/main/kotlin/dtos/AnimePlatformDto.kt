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
) {
    companion object {
        /**
         * Maps a stored platform source link to its API view.
         *
         * @param animePlatform source link read from the database.
         * @return the API view of [animePlatform].
         * @see AnimePlatform
         */
        fun toDto(animePlatform: AnimePlatform): AnimePlatformDto =
            AnimePlatformDto(
                platform = animePlatform.platform,
                platformId = animePlatform.platformId
            )
    }
}
