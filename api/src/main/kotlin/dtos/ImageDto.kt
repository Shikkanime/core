package fr.shikkanime.api.dtos

import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import kotlinx.serialization.Serializable

/**
 * API view of an artwork row.
 */
@Serializable
data class ImageDto(
    val type: ImageType,
    val url: String
)

/**
 * Maps a stored artwork row to its API view.
 *
 * @return the API view of this artwork row.
 * @see ImageDto
 */
fun Image.toDto(): ImageDto =
    ImageDto(
        type = type,
        url = url
    )
