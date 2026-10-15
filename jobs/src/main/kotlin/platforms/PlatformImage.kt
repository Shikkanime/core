package fr.shikkanime.jobs.platforms

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import kotlin.uuid.Uuid

/**
 * Artwork harvested from a streaming platform for one of its entities.
 *
 * @property type Role of the image, taken from the shared image type registry.
 * @property url Direct URL of the artwork as returned by the platform.
 */
data class PlatformImage(
    val type: ImageType,
    val url: String
) {
    /**
     * Maps this harvested artwork to the canonical image to persist.
     *
     * @param animeId identifier of the anime owning the image.
     * @return the [Image] to pass to the persistence.
     * @see Image
     */
    fun toModel(animeId: Uuid): Image =
        Image(
            id = Uuid.random(),
            createdAt = TimeUtils.now(),
            updatedAt = TimeUtils.now(),
            entityId = animeId,
            type = type,
            url = url
        )
}
