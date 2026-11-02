package fr.shikkanime.api.usecases

import fr.shikkanime.models.ImageType
import kotlin.uuid.Uuid

/**
 * Reads the WebP rendition of the artwork stored for an entity.
 *
 * @see [GetAttachmentResult]
 */
interface GetAttachmentUseCase {
    /**
     * Resolves the stored image and returns its WebP rendition.
     *
     * @param entityId identifier of the entity owning the artwork.
     * @param type type of the artwork to read.
     * @param ifNoneMatch `If-None-Match` header sent by the client, when present.
     * @return the rendition, a not-modified result, or a failure.
     * @see [GetAttachmentResult]
     */
    suspend fun execute(
        entityId: Uuid,
        type: ImageType,
        ifNoneMatch: String? = null
    ): GetAttachmentResult
}
