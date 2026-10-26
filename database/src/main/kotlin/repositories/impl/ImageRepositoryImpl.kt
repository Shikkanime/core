package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.ImageEntity
import fr.shikkanime.database.entities.ImageTable
import fr.shikkanime.database.repositories.ImageMaterialization
import fr.shikkanime.database.repositories.ImagePayloadRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [ImageRepository::class])
internal class ImageRepositoryImpl(
    private val imagePayloadRepository: ImagePayloadRepository
) : ImageRepository() {

    override fun findAllByEntityId(entityId: Uuid): List<Image> =
        ImageEntity.find { ImageTable.entityId eq entityId }
            .map(ImageEntity::toModel)

    override fun findAllByEntityIds(entityIds: List<Uuid>): Map<Uuid?, List<Image>> =
        ImageEntity.find { ImageTable.entityId inList entityIds }
            .map(ImageEntity::toModel)
            .groupBy(Image::entityId)

    override fun findByEntityIdAndType(entityId: Uuid, type: ImageType): Image? =
        findByIdAndType(entityId, type)?.toModel()

    override fun markMaterialized(
        imageId: Uuid,
        expectedUrl: String,
        materialization: ImageMaterialization
    ): Boolean =
        // One statement, guarded on the URL: a harvest that moved the artwork during the fetch
        // updates nothing, so its payload is never published
        ImageTable.update({ (ImageTable.id eq imageId) and (ImageTable.url eq expectedUrl) }) {
            it[version] = materialization.version
            it[width] = materialization.width
            it[height] = materialization.height
            it[byteSize] = materialization.byteSize.toLong()
            it[fetchedAt] = TimeUtils.now()
            it[sourceEtag] = materialization.sourceEtag
            it[sourceLastModified] = materialization.sourceLastModified
            it[failureCount] = 0
        } > 0

    override fun markFresh(imageId: Uuid, etag: String?, lastModified: String?) {
        findById(imageId)
            .ifExists {
                fetchedAt = TimeUtils.now()
                etag?.let { sourceEtag = it }
                lastModified?.let { sourceLastModified = it }
                applyFlush()
            }
    }

    override fun invalidate(imageId: Uuid) {
        // The bytes belong to the previous artwork: they go with the state that published them
        imagePayloadRepository.delete(imageId)

        findById(imageId)
            .ifExists {
                version = null
                fetchedAt = null
                sourceEtag = null
                sourceLastModified = null
                width = null
                height = null
                byteSize = null
                applyFlush()
            }
    }

    override fun recordFailure(imageId: Uuid) {
        findById(imageId)
            .ifExists {
                failureCount += 1
                applyFlush()
            }
    }

    /**
     * Finds one image row of an entity by its type.
     *
     * @param entityId identifier of the entity owning the image.
     * @param type type of the image to load.
     * @return the matching entity, or `null` when this image type is not set yet.
     * @see [Image]
     */
    private fun findByIdAndType(entityId: Uuid, type: ImageType): ImageEntity? =
        ImageEntity.find {
            (ImageTable.entityId eq entityId) and
                (ImageTable.type eq type)
        }
            .limit(1)
            .firstOrNull()

    override fun save(image: Image): Image =
        findByIdAndType(requireNotNull(image.entityId), image.type)
            .ifExists {
                if (url != image.url) {
                    url = image.url
                    updatedAt = TimeUtils.now()

                    // Another URL means another artwork: the stored payload belongs to the previous one
                    invalidate(id.value)
                }
            }
            .newIfNotExists(image.id) {
                entityId = requireNotNull(image.entityId)
                type = image.type
                url = image.url
            }
            .applyFlush()
            .toModel()
}
