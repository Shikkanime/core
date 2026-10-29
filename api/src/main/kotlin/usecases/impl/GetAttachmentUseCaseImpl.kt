package fr.shikkanime.api.usecases.impl

import fr.shikkanime.api.images.WebpConversionException
import fr.shikkanime.api.usecases.ConvertImageToWebpUseCase
import fr.shikkanime.api.usecases.GetAttachmentResult
import fr.shikkanime.api.usecases.GetAttachmentUseCase
import fr.shikkanime.database.services.ImageService
import fr.shikkanime.ktor.InvalidParameterException
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

private const val DEFAULT_QUALITY = 80

/**
 * Resolves the artwork of an entity and converts its source picture to WebP.
 *
 * The workflow is a read of the shared image table followed by the converter: it answers a
 * not-modified result before touching the network when the client already holds the current
 * validator.
 *
 * @param imageService persistence service resolving the artwork row.
 * @param convertImageToWebpUseCase converter reading and encoding the source picture.
 * @see [GetAttachmentUseCase]
 */
@Suppress("unused")
@Single
internal class GetAttachmentUseCaseImpl(
    private val imageService: ImageService,
    private val convertImageToWebpUseCase: ConvertImageToWebpUseCase
) : GetAttachmentUseCase {
    override suspend fun execute(
        entityId: Uuid,
        type: ImageType,
        ifNoneMatch: String?
    ): GetAttachmentResult {
        val image = imageService.findByEntityIdAndType(entityId, type)
            ?: return GetAttachmentResult.NotFound

        val etag = etagOf(image)

        if (matches(ifNoneMatch, etag))
            return GetAttachmentResult.NotModified(etag)

        return try {
            GetAttachmentResult.Found(
                payload = convertImageToWebpUseCase.convert(image.url, DEFAULT_QUALITY),
                etag = etag
            )
        } catch (exception: WebpConversionException) {
            GetAttachmentResult.Unavailable(exception.message ?: "The attachment could not be read")
        } catch (exception: InvalidParameterException) {
            GetAttachmentResult.Unavailable(exception.message)
        }
    }

    /**
     * Builds the validator of a stored image from its URL and update time.
     *
     * @param image stored image.
     * @return a stable, opaque entity tag.
     */
    private fun etagOf(image: Image): String =
        "${image.url.hashCode()}-${image.updatedAt}"

    /**
     * Tells whether the client already holds the current rendition.
     *
     * @param ifNoneMatch `If-None-Match` header, when present.
     * @param etag current validator.
     * @return `true` when the header matches [etag].
     */
    private fun matches(ifNoneMatch: String?, etag: String): Boolean {
        if (ifNoneMatch == null) return false
        if (ifNoneMatch.trim() == "*") return true
        return ifNoneMatch.split(',').any { candidate -> candidate.trim().trim('"') == etag }
    }
}
