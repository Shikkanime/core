package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.ImageEntity
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import kotlin.uuid.Uuid

/**
 * Persistence port of the shared image table, used by every entity owning artwork.
 *
 * Images are keyed by `(entityId, type)`: saving twice for the same key overwrites the stored URL
 * in place instead of adding a row.
 *
 * @see [ImageType]
 */
internal abstract class ImageRepository :
    ModelRepository<Uuid, ImageEntity, Image>(ImageEntity) {
    /**
     * Loads every image owned by an entity.
     *
     * @param entityId identifier of the owning entity.
     * @return the images of the owner, in no particular order.
     * @see [Image]
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
     * @see [Image]
     */
    abstract fun findAllByEntityIds(entityIds: List<Uuid>): Map<Uuid?, List<Image>>

    /**
     * Upserts one image, overwriting the URL when the owner already has this image type.
     *
     * The [Image.manual] flag carries the intent of the write: an automatic write never
     * replaces the URL of a manually managed image, while a manual write always applies and
     * marks the row.
     *
     * @param image image to persist; its identifier is only used for the creation.
     * @return the image as stored in database.
     * @see [Image]
     * @see [ImageType]
     */
    abstract fun save(image: Image): Image

    /**
     * Releases the manual management of one image, keeping its current URL.
     *
     * The next automatic write owns the image again and may replace its URL.
     *
     * @param entityId identifier of the entity owning the image.
     * @param type type of the image to release.
     * @return the image as stored in database, or `null` when this image type is not set.
     * @see [Image]
     * @see [ImageType]
     */
    abstract fun clearManual(entityId: Uuid, type: ImageType): Image?
}
