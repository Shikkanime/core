package fr.shikkanime.api.usecases.impl

import fr.shikkanime.api.images.WebpConversionException
import fr.shikkanime.api.usecases.ConvertImageToWebpUseCase
import fr.shikkanime.ktor.InvalidParameterException
import fr.shikkanime.ktor.createHttpClient
import fr.shikkanime.media.webp.WebpWriteParam
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageReader

private const val MAX_PAYLOAD_BYTES = 10L * 1024 * 1024
private const val MAX_PIXELS = 40_000_000L
private const val HTTP_PREFIX = "http://"
private const val HTTPS_PREFIX = "https://"

/**
 * Reads a picture from its address and encodes it to WebP, in memory.
 *
 * The picture is fetched with the framework's Ktor client, decoded by the ImageIO plugin, then
 * encoded by the same plugin: the conversion exercises the reader and the writer of the module, on a
 * payload that comes from the network rather than from a fixture.
 *
 * @see ConvertImageToWebpUseCase
 */
@Suppress("unused")
@Single
internal class ConvertImageToWebpUseCaseImpl : ConvertImageToWebpUseCase {
    private val client = createHttpClient()

    override suspend fun convert(sourceUrl: String, quality: Int): ByteArray {
        requireAddress(sourceUrl)
        requireQuality(quality)

        return encode(decode(fetch(sourceUrl)), quality)
    }

    /**
     * Refuses an address the endpoint does not serve.
     *
     * @param sourceUrl address given by the caller.
     * @throws InvalidParameterException when the address is not an absolute http or https URL.
     * @see [convert]
     */
    private fun requireAddress(sourceUrl: String) {
        if (!sourceUrl.startsWith(HTTP_PREFIX) && !sourceUrl.startsWith(HTTPS_PREFIX))
            throw InvalidParameterException("Parameter 'url' must be an absolute http or https address")
    }

    /**
     * Refuses a quality factor outside the scale libwebp uses.
     *
     * @param quality quality factor given by the caller.
     * @throws InvalidParameterException when the factor is outside 0-100.
     * @see [convert]
     */
    private fun requireQuality(quality: Int) {
        if (quality !in 0..100)
            throw InvalidParameterException("Parameter 'quality' must be between 0 and 100")
    }

    /**
     * Reads the picture published at an address.
     *
     * @param sourceUrl address to read.
     * @return the payload, capped at [MAX_PAYLOAD_BYTES].
     * @throws WebpConversionException when the host cannot be read, or answers something else than 200.
     * @see [convert]
     */
    private suspend fun fetch(sourceUrl: String): ByteArray {
        val response = try {
            client.get(sourceUrl)
        } catch (exception: Exception) {
            throw WebpConversionException("Could not read '$sourceUrl': ${exception.message}", exception)
        }

        if (response.status != HttpStatusCode.OK)
            throw WebpConversionException("'$sourceUrl' answered ${response.status}")

        val payload = response.bodyAsChannel()
            .readBuffer(MAX_PAYLOAD_BYTES + 1)
            .readByteArray()

        if (payload.size > MAX_PAYLOAD_BYTES)
            throw WebpConversionException("'$sourceUrl' exceeds $MAX_PAYLOAD_BYTES bytes")

        return payload
    }

    /**
     * Decodes a payload, refusing it when it declares too many pixels.
     *
     * The dimensions are read from the header: `ImageIO.read` allocates the whole raster, so a
     * payload announcing an enormous picture is refused before that allocation.
     *
     * @param payload bytes to decode.
     * @return the decoded picture.
     * @throws WebpConversionException when the payload holds no readable picture.
     * @see [convert]
     */
    private fun decode(payload: ByteArray): BufferedImage {
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(payload)).use { stream ->
                val readers = ImageIO.getImageReaders(stream)

                if (!readers.hasNext())
                    throw WebpConversionException("The payload holds no image")

                val reader: ImageReader = readers.next()

                try {
                    reader.input = stream

                    val pixels = reader.getWidth(0).toLong() * reader.getHeight(0)

                    if (pixels > MAX_PIXELS)
                        throw WebpConversionException("The picture declares $pixels pixels, over $MAX_PIXELS")

                    return reader.read(0)
                } finally {
                    reader.dispose()
                }
            }
        } catch (exception: WebpConversionException) {
            throw exception
        } catch (exception: Exception) {
            throw WebpConversionException("The payload is not a readable image", exception)
        }
    }

    /**
     * Encodes a picture to WebP.
     *
     * @param picture picture to encode.
     * @param quality quality factor between 0 and 100.
     * @return the encoded payload.
     * @throws WebpConversionException when no WebP writer is registered, or the encode fails.
     * @see [WebpWriteParam]
     */
    private fun encode(picture: BufferedImage, quality: Int): ByteArray {
        val writers = ImageIO.getImageWritersByFormatName("webp")

        if (!writers.hasNext())
            throw WebpConversionException("No WebP writer is registered")

        val writer = writers.next()

        try {
            val param = writer.defaultWriteParam as WebpWriteParam
            param.compressionType = WebpWriteParam.LOSSY
            param.compressionQuality = quality / 100f

            val output = ByteArrayOutputStream()

            ImageIO.createImageOutputStream(output).use { stream ->
                writer.output = stream
                writer.write(null, IIOImage(picture, null, null), param)
            }

            return output.toByteArray()
        } catch (exception: Exception) {
            throw WebpConversionException("The picture could not be encoded to WebP", exception)
        } finally {
            writer.dispose()
        }
    }
}
