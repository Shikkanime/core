package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Country

/**
 * Editorial content of an anime localized for a specific distribution.
 *
 * The platform distributes by country, so the localization is keyed by [Country]; the canonical
 * locale is derived from it through [Country.defaultLocale] during conversion.
 *
 * @property country Eligible country where this localized content applies.
 * @property title Human-readable title of the anime.
 * @property description Optional summary or synopsis of the anime, `null` when the platform does
 * not provide one yet.
 */
data class PlatformAnimeLocalization(
    val country: Country,
    val title: String,
    val description: String?
)
