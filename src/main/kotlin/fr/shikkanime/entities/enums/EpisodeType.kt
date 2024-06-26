package fr.shikkanime.entities.enums

enum class EpisodeType(val slug: String) {
    EPISODE("episode"),
    FILM("film"),
    SPECIAL("special"),
    SUMMARY("summary"),
    ;

    companion object {
        fun fromSlug(slug: String): EpisodeType {
            return entries.firstOrNull { it.slug == slug } ?: EPISODE
        }
    }
}