package fr.shikkanime.models

/**
 * Enumeration representing the editorial category of an episode release.
 *
 */
enum class EpisodeType(val slug: String) {
    EPISODE("episode"),
    FILM("film"),
    SPECIAL("special"),
    SUMMARY("summary"),
    SPIN_OFF("spin-off");

    companion object {
        /**
         * Resolves an [EpisodeType] from its [slug] representation, falling back to [EPISODE] if unknown.
         *
         * @param slug The slug identifier to look up.
         * @return The matching [EpisodeType], or [EPISODE] as safe default.
         * @see [EpisodeType]
         */
        fun fromSlug(slug: String): EpisodeType =
            entries.firstOrNull { it.slug == slug } ?: EPISODE
    }
}
