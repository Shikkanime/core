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
     * Loads one image of an owner.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the image to load.
     * @return the matching image, or `null` when this owner has no artwork of that type.
     * @see [Image]
     */
    abstract fun findByEntityIdAndType(entityId: Uuid, type: ImageType): Image?

    /**
     * Publishes a fresh materialization on an image.
     *
     * The version, the dimensions and the source validators are written in place: the image keeps
     * one published state at a time, and the version it exposes addresses the payload stored by
     * [ImagePayloadRepository].
     *
     * @param imageId identifier of the materialized image.
     * @param expectedUrl source URL the payload was read from.
     * @param materialization state published by the materialization.
     * @return `true` when the artwork was still served by [expectedUrl], `false` when the source moved.
     * @see [ImageMaterialization]
     */
    abstract fun markMaterialized(
        imageId: Uuid,
        expectedUrl: String,
        materialization: ImageMaterialization
    ): Boolean

    /**
     * Records that the source of an image was revalidated without any change.
     *
     * @param imageId identifier of the revalidated image.
     * @param etag source validator returned by the last fetch, when the platform provides one.
     * @param lastModified source modification date returned by the last fetch, when provided.
     * @see [Image]
     */
    abstract fun markFresh(imageId: Uuid, etag: String?, lastModified: String?)

    /**
     * Drops the payload of an image, and the version it published.
     *
     * Called when the source URL changes: the next read materializes the new artwork instead of
     * serving a payload that belongs to the previous one.
     *
     * @param imageId identifier of the image to invalidate.
     * @see [Image]
     */
    abstract fun invalidate(imageId: Uuid)

    /**
     * Records a failed materialization attempt.
     *
     * @param imageId identifier of the image that could not be materialized.
     * @see [Image]
     */
    abstract fun recordFailure(imageId: Uuid)

    /**
     * Upserts one image, overwriting the URL when the owner already has this image type.
     *
     * @param image image to persist; its identifier is only used for the creation.
     * @return the image as stored in database.
     * @see [Image]
     * @see [ImageType]
     */
    abstract fun save(image: Image): Image
}
