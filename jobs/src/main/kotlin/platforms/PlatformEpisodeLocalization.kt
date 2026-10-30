package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeLocalization

/**
 * Editorial content of an episode localized for a specific distribution.
 */
data class PlatformEpisodeLocalization(
    val country: Country,
    val title: String?,
    val description: String?
) {
    /**
     * Maps this harvested text to the canonical localization to persist.
     *
     * The distribution country decides the locale of the text it publishes, and the owning episode
     * does not exist yet at harvest time: the persistence attaches the localization to the episode
     * it just saved, exactly once.
     *
     * @return the [EpisodeLocalization] to pass to the persistence.
     * @see [EpisodeLocalization]
     */
    fun toModel(): EpisodeLocalization =
        EpisodeLocalization(
            id = null,
            createdAt = null,
            updatedAt = null,
            episodeId = null,
            locale = country.defaultLocale,
            title = title,
            description = description
        )
}
