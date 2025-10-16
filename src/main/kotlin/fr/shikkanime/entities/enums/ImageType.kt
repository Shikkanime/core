package fr.shikkanime.entities.enums

enum class ImageType(val width: Int, val height: Int) {
    THUMBNAIL(1560, 2340),
    BANNER(1920, 1080),
    CAROUSEL(1920, -1),
    MEMBER_PROFILE(128, 128),
}