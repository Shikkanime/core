package fr.shikkanime.api.usecases

import fr.shikkanime.models.ImageType
import kotlin.uuid.Uuid

/**
 * Resolves the artwork a client asks for, materializing it when it was never served.
 *
 * The controller keeps the HTTP concerns — parameters, statuses, headers — and this use case keeps
 * the workflow: load the published state, materialize it from its platform when it has none, and
 * revalidate it once it gets old.
 *
 * @see [ImageResult]
 */
interface FindImageUseCase {
    /**
     * Resolves one artwork at one of its versions.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the artwork.
     * @param version version the client asks for, as published in its address.
     * @param clientHoldsRequestedVersion whether the request revalidates that very version.
     * @return the payload to serve, the version to redirect to, the confirmation that the client
     * already holds it, or the reason it cannot be served.
     * @see [ImageResult]
     */
    suspend fun find(
        entityId: Uuid,
        type: ImageType,
        version: String,
        clientHoldsRequestedVersion: Boolean
    ): ImageResult
}
