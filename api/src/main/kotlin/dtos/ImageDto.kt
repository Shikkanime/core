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
) {
    companion object {
        /**
         * Maps a stored artwork row to its API view.
         *
         * @param image artwork row read from the database.
         * @return the API view of [image].
         * @see Image
         */
        fun toDto(image: Image): ImageDto =
            ImageDto(
                type = image.type,
                url = image.url
            )
    }
}
