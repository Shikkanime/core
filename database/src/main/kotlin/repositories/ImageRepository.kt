package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.ImageEntity
import fr.shikkanime.models.Image
import kotlin.uuid.Uuid

/**
 * Persistence port of the shared image table, used by every entity owning artwork.
 *
 * Images are keyed by `(entityId, type)`: saving twice for the same key overwrites the stored URL
 * in place instead of adding a row.
 *
 * @see fr.shikkanime.models.ImageType
 */
internal abstract class ImageRepository :
    ModelRepository<Uuid, ImageEntity, Image>(ImageEntity) {
    /**
     * Loads every image owned by an entity.
     *
     * @param entityId identifier of the owning entity.
     * @return the images of the owner, in no particular order.
     */
    abstract fun findAllByEntityId(entityId: Uuid): List<Image>

    /**
     * Upserts one image, overwriting the URL when the owner already has this image type.
     *
     * @param image image to persist; its identifier is only used for the creation.
     * @return the image as stored in database.
     */
    abstract fun save(image: Image): Image
}
