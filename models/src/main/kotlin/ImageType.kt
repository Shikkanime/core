package fr.shikkanime.models

/**
 * Registry of the image roles that can be stored for any entity, with canonical dimensions.
 *
 * @property width Canonical width in pixels, or -1 when the width is not constrained.
 * @property height Canonical height in pixels, or -1 when the height is not constrained.
 */
enum class ImageType(val width: Int, val height: Int) {
    THUMBNAIL(1560, 2340),
    BANNER(1920, 1080),
    TITLE(1920, -1)
}
