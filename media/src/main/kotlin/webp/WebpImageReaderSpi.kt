package fr.shikkanime.media.webp

import java.util.Locale
import javax.imageio.ImageReader
import javax.imageio.spi.ImageReaderSpi
import javax.imageio.stream.ImageInputStream

private const val VENDOR = "Shikkanime"
private const val VERSION = "1.0"
private const val HEADER_LENGTH = 12

/**
 * Declares WebP as a format ImageIO can read.
 *
 * Registered through `META-INF/services/javax.imageio.spi.ImageReaderSpi`, so `ImageIO.read` finds it
 * without the caller knowing the format is served by a native library.
 *
 * @see WebpImageReader
 */
class WebpImageReaderSpi : ImageReaderSpi(
    VENDOR,
    VERSION,
    arrayOf("webp", "WebP"),
    arrayOf("webp"),
    arrayOf("image/webp"),
    WebpImageReader::class.java.name,
    arrayOf(ImageInputStream::class.java),
    arrayOf(WebpImageWriterSpi::class.java.name),
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
     * Describes the reader.
     *
     * @param locale locale of the description.
     * @return a one-line description of the format.
     * @see [WebpImageReader]
     */
    override fun getDescription(locale: Locale): String = "WebP image reader (libwebp)"

    /**
     * Recognizes a WebP payload from its header.
     *
     * The stream is restored to its initial position: ImageIO probes it with several providers in a
     * row, and a probe that consumes bytes breaks the next one.
     *
     * @param source input stream to inspect.
     * @return `true` when the stream starts with a RIFF/WebP header.
     * @see [WebpImageReader]
     */
    override fun canDecodeInput(source: Any?): Boolean {
        val stream = source as? ImageInputStream ?: return false
        val position = runCatching { stream.streamPosition }.getOrNull() ?: return false

        return try {
            val header = ByteArray(HEADER_LENGTH)
            stream.readFully(header)
            String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
        } catch (_: Exception) {
            false
        } finally {
            runCatching { stream.seek(position) }
        }
    }

    /**
     * Creates the reader bound to this provider.
     *
     * @param extension provider-specific extension, ignored.
     * @return a new WebP reader.
     * @see [WebpImageReader]
     */
    override fun createReaderInstance(extension: Any?): ImageReader = WebpImageReader(this)
}
