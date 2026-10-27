package fr.shikkanime.media.webp

import java.util.Locale
import javax.imageio.ImageWriteParam

/**
 * Write parameters of the WebP format.
 *
 * The compression type selects lossy or lossless encoding, and the compression quality is the factor
 * libwebp expects, from 0 to 1 - 80 % by default, the middle of the range artwork uses.
 *
 * @param locale locale of the parameters.
 * @see WebpImageWriter
 */
class WebpWriteParam(locale: Locale) : ImageWriteParam(locale) {
    init {
        compressionTypes = arrayOf(LOSSY, LOSSLESS)
        compressionMode = MODE_EXPLICIT
        compressionType = LOSSY
        compressionQuality = DEFAULT_QUALITY
    }

    /**
     * Tells that the format supports compression.
     *
     * @return always `true`.
     * @see [WebpWriteParam]
     */
    override fun canWriteCompressed(): Boolean = true

    /**
     * Tells that the format supports a progressive mode.
     *
     * @return always `false`: WebP encodes in one pass.
     * @see [WebpWriteParam]
     */
    override fun canWriteProgressive(): Boolean = false

    companion object {
        /** Compression type encoding with loss, driven by the compression quality. */
        const val LOSSY = "Lossy"

        /** Compression type encoding without any loss, ignoring the compression quality. */
        const val LOSSLESS = "Lossless"

        private const val DEFAULT_QUALITY = 0.8f
    }
}
