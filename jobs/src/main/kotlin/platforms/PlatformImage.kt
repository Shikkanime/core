package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.ImageType

/**
 * Artwork harvested from a streaming platform for one of its entities.
 *
 * @property type Role of the image, taken from the shared image type registry.
 * @property url Direct URL of the artwork as returned by the platform.
 */
data class PlatformImage(
    val type: ImageType,
    val url: String
)
