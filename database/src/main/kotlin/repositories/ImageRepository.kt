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
     * Loads the artwork of several entities with one query, grouped by owner.
     *
     * The aggregate composition must not run one lookup per row: a whole page of animes resolves
     * its artwork through this single call.
     *
     * @param entityIds identifiers of the owning entities.
     * @return the images of each owner, an empty list for an owner without artwork.
     */
    abstract fun findAllByEntityIds(entityIds: List<Uuid>): Map<Uuid, List<Image>>

    /**
     * Upserts one image, overwriting the URL when the owner already has this image type.
     *
     * @param image image to persist; its identifier is only used for the creation.
     * @return the image as stored in database.
     */
    abstract fun save(image: Image): Image
}
