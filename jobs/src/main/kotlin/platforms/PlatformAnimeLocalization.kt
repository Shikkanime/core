package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.Country
import fr.shikkanime.models.Locale

/**
 * Editorial content of an anime localized for a specific distribution.
 *
 * The platform distributes by country, but a country distributes several locales: the locale is
 * the identity of the localization, so [PlatformAnimeLocalization] carries both the distribution
 * country and the locale it produces.
 */
data class PlatformAnimeLocalization(
    val country: Country,
    val locale: Locale,
    val title: String,
    val description: String?
) {
    /**
     * Maps this harvested text to the canonical localization to persist.
     *
     * A country distributes several locales (Switzerland speaks German, French and Italian), so
     * the locale, not the country, is the identity of a canonical localization. The owning anime
     * does not exist yet at harvest time: the persistence attaches the localization to the anime
     * it just saved, exactly once.
     *
     * @return the [AnimeLocalization] to pass to the persistence.
     * @see [AnimeLocalization]
     */
    fun toModel(): AnimeLocalization =
        AnimeLocalization(
            id = null,
            createdAt = null,
            updatedAt = null,
            animeId = null,
            locale = locale,
            title = title,
            description = description
        )
}
