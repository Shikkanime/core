package fr.shikkanime.api.usecases

/**
 * Outcome of a request for an artwork, as the controller has to answer it.
 *
 * @see [FindImageUseCase]
 */
sealed interface ImageResult {
    /**
     * The artwork is available.
     *
     * @property payload encoded WebP payload to send.
     * @property version published version of the payload, used as its entity tag.
     */
    class Found(val payload: ByteArray, val version: String) : ImageResult

    /**
     * The client already holds the current version.
     *
     * @property version version the client revalidated with.
     */
    class NotModified(val version: String) : ImageResult

    /**
     * The requested version is superseded.
     *
     * @property version version currently published.
     */
    class Moved(val version: String) : ImageResult

    /**
     * The entity has no artwork of that type.
     */
    data object NotFound : ImageResult

    /**
     * The platform could not be read, and no payload is stored.
     */
    data object Unavailable : ImageResult
}
