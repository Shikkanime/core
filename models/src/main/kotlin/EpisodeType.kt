package fr.shikkanime.models

/**
 * Enumeration representing the editorial category of an episode release.
 *
 * @property slug The canonical URL-friendly identifier used in routes and external payloads.
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
         */
        fun fromSlug(slug: String): EpisodeType =
            entries.firstOrNull { it.slug == slug } ?: EPISODE
    }
}
