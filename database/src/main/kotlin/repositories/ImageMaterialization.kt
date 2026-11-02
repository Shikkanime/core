package fr.shikkanime.database.repositories

/**
 * State published by a materialization, without the payload itself.
 *
 * The bytes belong to [ImagePayloadRepository]; this value carries what the image row needs to
 * address them: the version served in the URL, the dimensions of the encoded picture, and the
 * validators of the platform response that produced it.
 *
 * @property version short hash of the payload, published in the image URL.
 * @property width width of the encoded picture, in pixels.
 * @property height height of the encoded picture, in pixels.
 * @property byteSize size of the encoded payload, in bytes.
 * @property sourceEtag validator returned by the origin, when it provides one.
 * @property sourceLastModified modification date returned by the origin, when it provides one.
 */
class ImageMaterialization(
    val version: String,
    val width: Int,
    val height: Int,
    val byteSize: Int,
    val sourceEtag: String?,
    val sourceLastModified: String?
)
