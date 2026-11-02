package fr.shikkanime.media.webp

import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.util.Locale
import javax.imageio.IIOException
import javax.imageio.IIOImage
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.metadata.IIOMetadata
import javax.imageio.spi.ImageWriterSpi
import javax.imageio.stream.ImageOutputStream
import kotlin.math.roundToInt

private const val DEFAULT_QUALITY = 80

/**
 * Writes WebP payloads through libwebp.
 *
 * The writer hands the picture to the library as RGBA whatever its source type: libwebp drops the
 * alpha plane by itself when every pixel is opaque, which is what the format does natively.
 *
 * @param originatingProvider provider that declared this writer.
 * @see WebpImageWriterSpi
 */
class WebpImageWriter(
    originatingProvider: ImageWriterSpi?
) : ImageWriter(originatingProvider) {
    private var stream: ImageOutputStream? = null

    /**
     * Binds the output receiving the payload.
     *
     * @param output stream the encoded bytes are written to.
     * @throws IllegalArgumentException when the output is not an [ImageOutputStream].
     * @see [write]
     */
    override fun setOutput(output: Any?) {
        super.setOutput(output)
        stream = output as? ImageOutputStream
            ?: throw IllegalArgumentException("A WebP writer needs an ImageOutputStream")
    }

    /**
     * Returns the write parameters of the format.
     *
     * @return parameters exposing the lossy and lossless compression types.
     * @see [WebpWriteParam]
     */
    override fun getDefaultWriteParam(): ImageWriteParam = WebpWriteParam(locale ?: Locale.getDefault())

    /**
     * Writes one image with the default parameters.
     *
     * @param sequence image to write.
     * @throws IIOException when libwebp cannot encode the picture.
     * @see [write]
     */
    override fun write(sequence: IIOImage) = write(null, sequence, getDefaultWriteParam())

    /**
     * Encodes and writes the picture.
     *
     * @param streamMetadata metadata of the stream, ignored: the format carries none here.
     * @param image image to write.
     * @param param write parameters, or `null` for the defaults.
     * @throws IIOException when libwebp cannot encode the picture.
     * @see [WebpWriteParam]
     */
    override fun write(streamMetadata: IIOMetadata?, image: IIOImage, param: ImageWriteParam?) {
        val output = stream ?: throw IllegalStateException("The writer has no output stream")
        val picture = toBufferedImage(image.renderedImage)
        val parameters = param ?: getDefaultWriteParam()

        val payload = try {
            if (parameters is WebpWriteParam && parameters.compressionType == WebpWriteParam.LOSSLESS)
                LibWebp.encodeLossless(picture)
            else
                LibWebp.encode(picture, qualityOf(parameters))
        } catch (exception: WebpCodecException) {
            throw IIOException(exception.message, exception)
        }

        output.write(payload)
        output.flush()
    }

    /**
     * Returns the default stream metadata of the format.
     *
     * @param param write parameters, unused.
     * @return always `null`: WebP carries no stream metadata here.
     * @see [write]
     */
    override fun getDefaultStreamMetadata(param: ImageWriteParam?): IIOMetadata? = null

    /**
     * Returns the default image metadata of the format.
     *
     * @param type image type to describe, unused.
     * @param param write parameters, unused.
     * @return always `null`: the writer stores no metadata.
     * @see [write]
     */
    override fun getDefaultImageMetadata(type: ImageTypeSpecifier?, param: ImageWriteParam?): IIOMetadata? = null

    /**
     * Converts stream metadata to the format of this writer.
     *
     * @param metadata metadata to convert, unused.
     * @param param write parameters, unused.
     * @return the metadata unchanged.
     * @see [write]
     */
    override fun convertStreamMetadata(metadata: IIOMetadata?, param: ImageWriteParam?): IIOMetadata? = metadata

    /**
     * Converts image metadata to the format of this writer.
     *
     * @param metadata metadata to convert, unused.
     * @param type image type to describe, unused.
     * @param param write parameters, unused.
     * @return the metadata unchanged.
     * @see [write]
     */
    override fun convertImageMetadata(
        metadata: IIOMetadata?,
        type: ImageTypeSpecifier?,
        param: ImageWriteParam?
    ): IIOMetadata? = metadata

    /**
     * Converts a rendered image to the buffer the encoder reads.
     *
     * @param source image to convert.
     * @return the source itself when it already is a `BufferedImage`, a copy otherwise.
     * @see [write]
     */
    private fun toBufferedImage(source: RenderedImage): BufferedImage {
        if (source is BufferedImage)
            return source

        val converted = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = converted.createGraphics()

        try {
            graphics.drawRenderedImage(source, null)
        } finally {
            graphics.dispose()
        }

        return converted
    }

    /**
     * Maps the ImageIO compression quality onto the factor libwebp expects.
     *
     * @param param parameters of the write.
     * @return a quality between 0 and 100.
     * @see [WebpWriteParam]
     */
    private fun qualityOf(param: ImageWriteParam): Int =
        if (param.compressionMode == ImageWriteParam.MODE_EXPLICIT && param.canWriteCompressed())
            (param.compressionQuality * 100f).roundToInt().coerceIn(0, 100)
        else
            DEFAULT_QUALITY
}
