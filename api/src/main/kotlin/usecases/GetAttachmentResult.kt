package fr.shikkanime.api.usecases

/**
 * Result of reading the WebP rendition of a stored attachment.
 */
sealed interface GetAttachmentResult {
    /**
     * The rendition is available.
     *
     * @property payload encoded WebP bytes.
     * @property etag validator of the stored image, used by the HTTP cache.
     */
    class Found(val payload: ByteArray, val etag: String) : GetAttachmentResult

    /**
     * The client already holds the current rendition.
     *
     * @property etag validator of the stored image.
     */
    data class NotModified(val etag: String) : GetAttachmentResult

    /** No image is stored for the requested entity and type. */
    data object NotFound : GetAttachmentResult

    /**
     * The source picture could not be read or encoded.
     *
     * @property message user-safe failure description.
     */
    data class Unavailable(val message: String) : GetAttachmentResult
}
