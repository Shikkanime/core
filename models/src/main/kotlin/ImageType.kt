package fr.shikkanime.models

/**
 * Registry of the image roles that can be stored for any entity, with canonical dimensions.
 *
 */
enum class ImageType(val width: Int, val height: Int) {
    THUMBNAIL(1560, 2340),
    BANNER(1920, 1080),
    TITLE(1920, -1)
}
