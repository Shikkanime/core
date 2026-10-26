package fr.shikkanime.database.services.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.media.ImageCodec
import fr.shikkanime.database.repositories.ImageMaterialization
import fr.shikkanime.database.repositories.ImagePayloadRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.ImagePayload
import fr.shikkanime.database.services.ImageService
import fr.shikkanime.database.services.ImageSource
import fr.shikkanime.database.services.ImageWithPayload
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage
import java.security.MessageDigest
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [ImageService::class])
internal class ImageServiceImpl(
    private val imageRepository: ImageRepository,
    private val imagePayloadRepository: ImagePayloadRepository
) : ImageService {

    override fun findByEntityIdAndType(entityId: Uuid, type: ImageType): Image? =
        // Resolve the artwork registered for this owner and type
        imageRepository.findByEntityIdAndType(entityId, type)

    override fun findWithPayload(entityId: Uuid, type: ImageType, expectedVersion: String?): ImageWithPayload? {
        // One transaction: the row and its bytes come from the same snapshot
        val image = imageRepository.findByEntityIdAndType(entityId, type) ?: return null

        // A redirected or revalidated request does not need the bytes at all
        if (expectedVersion != null && image.version != expectedVersion)
            return ImageWithPayload(image, null)

        val imageId = requireNotNull(image.id) { "An image without identifier has no payload" }

        return ImageWithPayload(image, imagePayloadRepository.findByImageId(imageId))
    }

    override fun findPayload(image: Image): ByteArray? =
        imagePayloadRepository.findByImageId(
            requireNotNull(image.id) { "An image without identifier has no payload" }
        )

    override fun store(image: Image, source: ImageSource): ImagePayload {
        val imageId = requireNotNull(image.id) { "An image without identifier cannot own a payload" }

        // Decode the platform bytes, then bring them to the canonical dimensions of the type
        val decoded = ImageCodec.decode(source.bytes)
        val resized = ImageCodec.resize(decoded, image.type.width, image.type.height)

        // A type without height is flat artwork, where lossy encoding frays the transparent edges
        val encoded = if (image.type.height <= 0)
            ImageCodec.encodeLossless(resized)
        else
            ImageCodec.encodeLossy(resized, qualityOf(resized))

        val materialization = ImageMaterialization(
            version = versionOf(encoded),
            width = resized.width,
            height = resized.height,
            byteSize = encoded.size,
            sourceEtag = source.etag,
            sourceLastModified = source.lastModified
        )

        // The guard is the write itself: a source that moved during the fetch updates nothing
        require(imageRepository.markMaterialized(imageId, image.url, materialization)) {
            "The artwork source changed while it was being fetched"
        }

        imagePayloadRepository.save(imageId, encoded)

        return ImagePayload(
            image.copy(
                version = materialization.version,
                width = materialization.width,
                height = materialization.height,
                fetchedAt = TimeUtils.now(),
                sourceEtag = materialization.sourceEtag,
                sourceLastModified = materialization.sourceLastModified
            ),
            encoded
        )
    }

    override fun markFresh(image: Image, etag: String?, lastModified: String?) =
        imageRepository.markFresh(
            requireNotNull(image.id) { "An image without identifier cannot be revalidated" },
            etag,
            lastModified
        )

    override fun recordFailure(image: Image) =
        imageRepository.recordFailure(requireNotNull(image.id) { "An image without identifier cannot fail" })

    override fun loadImage(entityId: Uuid, type: ImageType): BufferedImage? {
        val image = imageRepository.findByEntityIdAndType(entityId, type) ?: return null
        val payload = findPayload(image) ?: return null

        // The payload is the only copy kept: decoding it must not need the platform back
        return ImageCodec.decode(payload)
    }

    /**
     * Picks the WebP quality of an image.
     *
     * Photographs keep the historical 75-85 % range; artwork carrying transparency is encoded at the
     * top of that range, because a lossy edge over a transparent background shows its artefacts as
     * soon as the picture is composed on another background.
     *
     * @param image picture about to be encoded.
     * @return the quality factor to hand to the codec.
     * @see [ImageCodec.encodeLossy]
     */
    private fun qualityOf(image: BufferedImage): Int =
        if (image.colorModel.hasAlpha()) ALPHA_QUALITY else QUALITY

    /**
     * Computes the published version of a payload.
     *
     * The version addresses the bytes: two different payloads never share it, so a refresh publishes
     * a new URL and every cache in front of it becomes stale without a purge.
     *
     * @param payload encoded payload to address.
     * @return the first 8 bytes of its SHA-256 digest, as 16 hexadecimal characters.
     * @see [Image]
     */
    private fun versionOf(payload: ByteArray): String =
        MessageDigest.getInstance(SHA_256)
            .digest(payload)
            .take(VERSION_BYTES)
            .joinToString(separator = "") { "%02x".format(it) }

    companion object {
        private const val QUALITY = 80
        private const val ALPHA_QUALITY = 85
        private const val SHA_256 = "SHA-256"
        private const val VERSION_BYTES = 8
    }
}
