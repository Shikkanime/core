package fr.shikkanime.media.webp

import java.util.Locale
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriter
import javax.imageio.spi.ImageWriterSpi
import javax.imageio.stream.ImageOutputStream

private const val VENDOR = "Shikkanime"
private const val VERSION = "1.0"

/**
 * Declares WebP as a format ImageIO can write.
 *
 * Registered through `META-INF/services/javax.imageio.spi.ImageWriterSpi`, so `ImageIO.write` finds it
 * without the caller knowing the format is served by a native library.
 *
 * @see WebpImageWriter
 */
class WebpImageWriterSpi : ImageWriterSpi(
    VENDOR,
    VERSION,
    arrayOf("webp", "WebP"),
    arrayOf("webp"),
    arrayOf("image/webp"),
    WebpImageWriter::class.java.name,
    arrayOf(ImageOutputStream::class.java),
    arrayOf(WebpImageReaderSpi::class.java.name),
    false,
    null,
    null,
    null,
    null,
    false,
    null,
    null,
    null,
    null
) {
    /**
     * Describes the writer.
     *
     * @param locale locale of the description.
     * @return a one-line description of the format.
     * @see [WebpImageWriter]
     */
    override fun getDescription(locale: Locale): String = "WebP image writer (libwebp)"

    /**
     * Tells whether an image type can be written.
     *
     * Any rendered image is accepted: it is converted to RGBA before encoding, so refusing a type
     * would only force the caller to convert what the writer converts anyway.
     *
     * @param type image type to test.
     * @return `true` for every image type.
     * @see [WebpImageWriter]
     */
    override fun canEncodeImage(type: ImageTypeSpecifier?): Boolean = true

    /**
     * Creates the writer bound to this provider.
     *
     * @param extension provider-specific extension, ignored.
     * @return a new WebP writer.
     * @see [WebpImageWriter]
     */
    override fun createWriterInstance(extension: Any?): ImageWriter = WebpImageWriter(this)
}
