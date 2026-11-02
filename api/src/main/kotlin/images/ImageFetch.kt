package fr.shikkanime.api.images

/**
 * Outcome of reading an artwork from its platform.
 *
 * @see [ImageFetcher]
 */
sealed interface ImageFetch {
    /**
     * The platform served the artwork.
     *
     * @property bytes artwork as served by the platform.
     * @property etag validator returned by the platform, when it provides one.
     * @property lastModified modification date returned by the platform, when it provides one.
     */
    class Fetched(
        val bytes: ByteArray,
        val etag: String?,
        val lastModified: String?
    ) : ImageFetch

    /**
     * The platform confirmed that the stored artwork is still current.
     *
     * @property etag validator returned by the platform, when it provides one.
     * @property lastModified modification date returned by the platform, when it provides one.
     */
    class NotModified(
        val etag: String?,
        val lastModified: String?
    ) : ImageFetch

    /**
     * The platform could not be read: the artwork stays as it is.
     */
    data object Failed : ImageFetch
}
