package fr.shikkanime.media.webp

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOException
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadata
import javax.imageio.spi.ImageReaderSpi
import javax.imageio.stream.ImageInputStream

private const val READ_BUFFER_LENGTH = 8192
private const val SINGLE_IMAGE = 0

/**
 * Reads WebP payloads through libwebp.
 *
 * The header answers the dimensions without decoding anything, so a caller that only needs the size
 * of an artwork never pays for its pixels.
 *
 * @param originatingProvider provider that declared this reader.
 * @see WebpImageReaderSpi
 */
class WebpImageReader(
    originatingProvider: ImageReaderSpi?
) : ImageReader(originatingProvider) {
    private var stream: ImageInputStream? = null
    private var header: WebpInfo? = null

    /**
     * Binds the payload to read.
     *
     * @param source input stream holding a WebP payload.
     * @param seekForwardOnly whether the stream may be read once, forward only.
     * @param ignoreMetadata whether metadata may be ignored.
     * @throws IllegalArgumentException when the source is not an [ImageInputStream].
     * @see [read]
     */
    override fun setInput(source: Any?, seekForwardOnly: Boolean, ignoreMetadata: Boolean) {
        super.setInput(source, seekForwardOnly, ignoreMetadata)
        header = null
        stream = source as? ImageInputStream
            ?: throw IllegalArgumentException("A WebP reader needs an ImageInputStream")
    }

    /**
     * Returns the number of images in the payload.
     *
     * @param allowSearch whether the reader may search the stream.
     * @return always one: animation is out of scope.
     * @see [read]
     */
    override fun getNumImages(allowSearch: Boolean): Int = 1

    /**
     * Returns the width of the picture, read from the header.
     *
     * @param imageIndex index of the image, always 0.
     * @return the width in pixels.
     * @see [getHeight]
     */
    override fun getWidth(imageIndex: Int): Int {
        requireIndex(imageIndex)
        return info().width
    }

    /**
     * Returns the height of the picture, read from the header.
     *
     * @param imageIndex index of the image, always 0.
     * @return the height in pixels.
     * @see [getWidth]
     */
    override fun getHeight(imageIndex: Int): Int {
        requireIndex(imageIndex)
        return info().height
    }

    /**
     * Lists the image types this payload decodes to.
     *
     * @param imageIndex index of the image, always 0.
     * @return ARGB when the payload carries transparency, RGB otherwise.
     * @see [read]
     */
    override fun getImageTypes(imageIndex: Int): Iterator<ImageTypeSpecifier> {
        requireIndex(imageIndex)

        val type = if (info().hasAlpha)
            BufferedImage.TYPE_INT_ARGB
        else
            BufferedImage.TYPE_INT_RGB

        return listOf(ImageTypeSpecifier.createFromBufferedImageType(type)).iterator()
    }

    /**
     * Tells that random access is cheap for this format.
     *
     * @param imageIndex index of the image, always 0.
     * @return always `true`.
     * @see [read]
     */
    override fun isRandomAccessEasy(imageIndex: Int): Boolean = true

    /**
     * Decodes the picture.
     *
     * A source region is served by cropping the decoded picture, and a destination image is filled
     * instead of allocating a new one.
     *
     * @param imageIndex index of the image, always 0.
     * @param param read parameters, or `null` for the defaults.
     * @return the decoded picture.
     * @throws IIOException when libwebp cannot decode the payload.
     * @see [getWidth]
     */
    override fun read(imageIndex: Int, param: ImageReadParam?): BufferedImage {
        requireIndex(imageIndex)

        val decoded = try {
            LibWebp.decode(payload())
        } catch (exception: WebpCodecException) {
            throw IIOException(exception.message, exception)
        }

        val cropped = param?.sourceRegion?.let { region ->
            decoded.getSubimage(region.x, region.y, region.width, region.height)
        } ?: decoded

        val destination = param?.destination ?: return cropped

        val graphics = destination.createGraphics()
        try {
            graphics.drawImage(cropped, 0, 0, null)
        } finally {
            graphics.dispose()
        }

        return destination
    }

    /**
     * Returns the stream metadata of the payload.
     *
     * @return always `null`: WebP carries no stream metadata here.
     * @see [read]
     */
    override fun getStreamMetadata(): IIOMetadata? = null

    /**
     * Returns the metadata of one image.
     *
     * @param imageIndex index of the image, always 0.
     * @return always `null`: the reader exposes no metadata.
     * @see [read]
     */
    override fun getImageMetadata(imageIndex: Int): IIOMetadata? = null

    /**
     * Releases the state of this reader.
     *
     * The stream belongs to the caller: ImageIO readers release what they hold, not the input.
     *
     * @see [setInput]
     */
    override fun dispose() {
        stream = null
        header = null
    }

    /**
     * Returns the description of the payload, reading it once.
     *
     * @return the dimensions and the alpha plane of the bound payload.
     * @see [getWidth]
     */
    private fun info(): WebpInfo =
        header ?: LibWebp.readInfo(payload()).also { header = it }

    /**
     * Copies the bound payload into memory, from its first byte.
     *
     * @return every byte of the payload.
     * @throws IllegalStateException when no input is bound to the reader.
     * @see [read]
     */
    private fun payload(): ByteArray {
        val input = stream ?: throw IllegalStateException("The reader has no input stream")

        input.seek(0)

        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(READ_BUFFER_LENGTH)

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            bytes.write(buffer, 0, read)
        }

        return bytes.toByteArray()
    }

    /**
     * Refuses an index the format cannot hold.
     *
     * @param imageIndex index asked by the caller.
     * @throws IndexOutOfBoundsException when the index is not the single image.
     * @see [read]
     */
    private fun requireIndex(imageIndex: Int) {
        if (imageIndex != SINGLE_IMAGE)
            throw IndexOutOfBoundsException("Image $imageIndex: this format holds a single image")
    }
}
