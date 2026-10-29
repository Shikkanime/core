package fr.shikkanime.api.usecases

import fr.shikkanime.api.images.WebpConversionException
import fr.shikkanime.ktor.InvalidParameterException

/**
 * Reads a picture from an address and encodes it to WebP.
 *
 * This is the low-level converter used by the attachment endpoint: it reads the source picture
 * with the framework's Ktor client, decodes it with ImageIO, and encodes the resulting image with
 * the media module's WebP writer.
 *
 * @see [GetAttachmentUseCase]
 */
interface ConvertImageToWebpUseCase {
    /**
     * Converts the picture published at [sourceUrl] to WebP.
     *
     * @param sourceUrl absolute http or https address of the picture.
     * @param quality quality factor between 0 and 100.
     * @return the encoded WebP payload.
     * @throws InvalidParameterException when the address or the quality is refused.
     * @throws WebpConversionException when the picture cannot be read or encoded.
     */
    suspend fun convert(sourceUrl: String, quality: Int): ByteArray
}
