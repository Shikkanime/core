package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType

/**
 * Artwork harvested from a streaming platform for one of its entities.
 *
 */
data class PlatformImage(
    val type: ImageType,
    val url: String
) {
    /**
     * Maps this harvested artwork to the canonical image to persist.
     *
     * The owner does not exist yet at harvest time: the persistence attaches the image to the anime
     * it just saved, exactly once.
     *
     * @return the [Image] to pass to the persistence.
     * @see [Image]
     */
    fun toModel(): Image =
        Image(
            id = null,
            createdAt = null,
            updatedAt = null,
            entityId = null,
            type = type,
            url = url
        )
}
