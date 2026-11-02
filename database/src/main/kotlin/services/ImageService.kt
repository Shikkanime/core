package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import kotlin.uuid.Uuid

/**
 * Application service reading the artwork stored for an entity.
 *
 * Each method runs inside a single database transaction and returns domain models only; the
 * persistence entity stays internal to the `:database` module.
 *
 * @see [Image]
 */
interface ImageService {
    /**
     * Loads the image of an entity for one type.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the image to load.
     * @return the stored image, or `null` when this type is not set yet.
     * @see [Image]
     */
    @Transactional
    fun findByEntityIdAndType(entityId: Uuid, type: ImageType): Image?
}
