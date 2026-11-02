package fr.shikkanime.api.usecases.impl

import fr.shikkanime.api.images.ImageMaterializer
import fr.shikkanime.api.usecases.FindImageUseCase
import fr.shikkanime.api.usecases.ImageResult
import fr.shikkanime.database.services.ImagePayload
import fr.shikkanime.database.services.ImageService
import fr.shikkanime.models.ImageType
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single
internal class FindImageUseCaseImpl(
    private val imageService: ImageService,
    private val imageMaterializer: ImageMaterializer
) : FindImageUseCase {

    override suspend fun find(
        entityId: Uuid,
        type: ImageType,
        version: String,
        clientHoldsRequestedVersion: Boolean
    ): ImageResult {
        // One transaction answers the row, and its bytes only when the version matches
        val found = imageService.findWithPayload(entityId, type, version) ?: return ImageResult.NotFound

        val current = found.image.version
        if (current != null && current != version)
            return ImageResult.Moved(current)

        // The client already holds these bytes: the row answers, the payload is never read
        if (current != null && clientHoldsRequestedVersion)
            return ImageResult.NotModified(current)

        // A payload can be missing behind a published version (database copied without it): rebuild it
        val stored = found.payload
        val payload = if (stored == null)
            imageMaterializer.materialize(found.image) ?: return ImageResult.Unavailable
        else
            ImagePayload(found.image, stored)

        // The response is on its way: an outdated artwork is revalidated afterwards
        imageMaterializer.revalidateIfStale(payload.image)

        return ImageResult.Found(payload.bytes, requireNotNull(payload.image.version))
    }
}
