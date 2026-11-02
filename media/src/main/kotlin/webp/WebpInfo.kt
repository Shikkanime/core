package fr.shikkanime.media.webp

/**
 * Description of a WebP payload, read from its header.
 *
 * @property width width of the picture, in pixels.
 * @property height height of the picture, in pixels.
 * @property hasAlpha whether the payload carries an alpha plane.
 */
class WebpInfo(
    val width: Int,
    val height: Int,
    val hasAlpha: Boolean
)
