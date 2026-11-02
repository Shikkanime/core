package fr.shikkanime.database.services

import fr.shikkanime.models.Image

/**
 * One artwork with its encoded payload, read in the same transaction.
 *
 * Both are returned together because they must agree: serving the bytes of one version under the
 * address of another would poison every cache in front of the API. The payload is `null` when the
 * artwork was never materialized, or when its published version has no bytes yet.
 *
 * @property image image row describing the artwork.
 * @property payload encoded WebP payload, when one is stored for this version.
 */
class ImageWithPayload(
    val image: Image,
    val payload: ByteArray?
)
