package fr.shikkanime.database.services

/**
 * Source bytes of an artwork, with the validators the origin returned with them.
 *
 * The validators are what makes a later refresh conditional: an origin answering `304` costs a
 * round trip instead of a download and a re-encode.
 *
 * @property bytes artwork as served by the platform.
 * @property etag `ETag` header of the response, when the platform sends one.
 * @property lastModified `Last-Modified` header of the response, when the platform sends one.
 */
class ImageSource(
    val bytes: ByteArray,
    val etag: String?,
    val lastModified: String?
)
