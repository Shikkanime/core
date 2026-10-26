package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.ImagePayloadEntity
import fr.shikkanime.exposed.repositories.EntityRepository
import kotlin.uuid.Uuid

/**
 * Persistence port of the encoded artwork payloads.
 *
 * A payload is keyed by the identifier of its image, in a table of its own: a catalogue read that
 * only needs URLs never touches the bytes, and the payload of an artwork is updated in place.
 *
 * @see [ImagePayloadEntity]
 */
internal abstract class ImagePayloadRepository :
    EntityRepository<Uuid, ImagePayloadEntity>(ImagePayloadEntity) {
    /**
     * Loads the encoded payload of an image.
     *
     * @param imageId identifier of the image owning the payload.
     * @return the stored payload, or `null` when the artwork was never materialized.
     * @see [ImagePayloadEntity]
     */
    abstract fun findByImageId(imageId: Uuid): ByteArray?

    /**
     * Stores the encoded payload of an image, replacing the previous one in place.
     *
     * @param imageId identifier of the image owning the payload.
     * @param bytes encoded WebP payload.
     * @see [ImagePayloadEntity]
     */
    abstract fun save(imageId: Uuid, bytes: ByteArray)

    /**
     * Drops the encoded payload of an image.
     *
     * Called when the source URL changes: the stored bytes belong to the previous artwork and must
     * not be served under the new one.
     *
     * @param imageId identifier of the image losing its payload.
     * @see [ImageRepository.save]
     */
    abstract fun delete(imageId: Uuid)
}
