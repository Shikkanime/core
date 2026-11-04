package fr.shikkanime.admin.dtos.image

import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import kotlinx.serialization.Serializable

/**
 * Artwork of an anime or an episode, as the administration reads it.
 */
@Serializable
data class AdminImageDto(
    val type: ImageType,
    val url: String
)

/**
 * Maps a stored image to its administration view.
 *
 * @receiver stored image.
 * @return the administration view of this image.
 * @see [AdminImageDto]
 */
fun Image.toAdminDto(): AdminImageDto =
    AdminImageDto(
        type = type,
        url = url
    )
