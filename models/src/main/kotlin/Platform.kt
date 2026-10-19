package fr.shikkanime.models

/**
 * Streaming platforms an anime can come from.
 *
 * The lowest index wins the editorial data, so the harvest order no longer decides who overwrites
 * whom.
 */
enum class Platform(val sortIndex: Short) {
    ANIMATION_DIGITAL_NETWORK(0),
    CRUNCHYROLL(1),
    DISNEY_PLUS(2),
    NETFLIX(3),
    PRIME_VIDEO(4),
}
