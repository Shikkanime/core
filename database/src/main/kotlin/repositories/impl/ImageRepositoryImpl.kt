package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.ImageEntity
import fr.shikkanime.database.entities.ImageTable
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [ImageRepository::class])
internal class ImageRepositoryImpl : ImageRepository() {

    override fun findAllByEntityId(entityId: Uuid): List<Image> =
        ImageEntity.find { ImageTable.entityId eq entityId }
            .map(ImageEntity::toModel)

    /**
     * Finds one image row of an entity by its type.
     *
     * @param entityId identifier of the entity owning the image.
     * @param type type of the image to load.
     * @return the matching entity, or `null` when this image type is not set yet.
     */
    private fun findByIdAndType(entityId: Uuid, type: ImageType): ImageEntity? =
        ImageEntity.find {
            (ImageTable.entityId eq entityId) and
                (ImageTable.type eq type)
        }
            .limit(1)
            .firstOrNull()

    override fun save(image: Image): Image =
        findByIdAndType(image.entityId, image.type)
            .ifExists {
                url = image.url
                updatedAt = TimeUtils.now()
            }
            .newIfNotExists(image.id) {
                entityId = image.entityId
                type = image.type
                url = image.url
            }
            .applyFlush()
            .toModel()
}
