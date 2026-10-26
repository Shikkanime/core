package fr.shikkanime.database.services

import fr.shikkanime.models.Image

/**
 * Materialized artwork: the row describing it, and the encoded payload it publishes.
 *
 * @property image image row carrying the version, the dimensions and the source validators.
 * @property bytes encoded WebP payload.
 */
class ImagePayload(
    val image: Image,
    val bytes: ByteArray
)
