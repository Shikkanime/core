package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import java.awt.image.BufferedImage
import kotlin.uuid.Uuid

/**
 * Application service materializing the artwork an entity references.
 *
 * An image row only carries the source URL of its platform. The bytes are produced on demand, once:
 * the platform is read, the picture is resized to the canonical dimensions of its [ImageType],
 * encoded to WebP, and stored next to the row. Nothing is downloaded ahead of a request, and the
 * encoded payload is the only copy kept afterwards.
 *
 * Consume it through the Koin bindings or a [fr.shikkanime.exposed.TransactionalProxy] so the
 * [Transactional] methods are intercepted.
 */
interface ImageService {
    /**
     * Loads the image row of an entity, without its payload.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the artwork to load.
     * @return the image row, or `null` when the owner has no artwork of that type.
     * @see [Image]
     */
    @Transactional
    fun findByEntityIdAndType(entityId: Uuid, type: ImageType): Image?

    /**
     * Loads an artwork and its payload in one transaction.
     *
     * A single read answers both: the row and the bytes must belong to the same version, and a
     * serving path that reads them separately costs one query too many per request.
     *
     * The payload is only read when its version is the one asked for: a redirected or revalidated
     * request answers from the row alone.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the artwork to load.
     * @param expectedVersion version the client asks for, `null` when it asks for the current one.
     * @return the artwork with its payload, the payload being `null` when it is not materialized or
     * does not belong to [expectedVersion].
     * @see [ImageWithPayload]
     */
    @Transactional
    fun findWithPayload(entityId: Uuid, type: ImageType, expectedVersion: String?): ImageWithPayload?

    /**
     * Loads the encoded payload of an image.
     *
     * @param image image whose payload to load.
     * @return the stored WebP payload, or `null` when the artwork was never materialized.
     * @see [Image]
     */
    @Transactional
    fun findPayload(image: Image): ByteArray?

    /**
     * Materializes an artwork from its source bytes.
     *
     * The picture is decoded, resized to the canonical dimensions of its type and encoded to WebP:
     * lossy for photographs, lossless for the artwork types carrying flat transparency, where lossy
     * would fray the edges of a logo.
     *
     * @param image artwork row the payload belongs to.
     * @param source bytes of the platform artwork, with the validators of the response.
     * @return the stored image, with its published payload.
     * @throws IllegalArgumentException when the source URL moved while the bytes were being fetched,
     * or when the bytes hold no image.
     * @see [ImagePayload]
     */
    @Transactional
    fun store(image: Image, source: ImageSource): ImagePayload

    /**
     * Records that an artwork was revalidated without any change.
     *
     * @param image image that was revalidated.
     * @param etag validator returned by the origin, when it provides one.
     * @param lastModified modification date returned by the origin, when it provides one.
     * @see [Image]
     */
    @Transactional
    fun markFresh(image: Image, etag: String?, lastModified: String?)

    /**
     * Records a failed materialization attempt.
     *
     * @param image image that could not be materialized.
     * @see [Image]
     */
    @Transactional
    fun recordFailure(image: Image)

    /**
     * Decodes the stored artwork of an entity, without any platform request.
     *
     * This is what the media composition reads: the WebP payload is decoded from the database, so a
     * picture already materialized is never fetched again.
     *
     * @param entityId identifier of the owning entity.
     * @param type type of the artwork to decode.
     * @return the decoded picture, or `null` when the artwork is not materialized.
     * @see [ImageService.store]
     */
    @Transactional
    fun loadImage(entityId: Uuid, type: ImageType): BufferedImage?
}
