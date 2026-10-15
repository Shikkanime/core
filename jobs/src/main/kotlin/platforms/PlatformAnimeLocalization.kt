package fr.shikkanime.jobs.platforms

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.Country
import kotlin.uuid.Uuid

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
) {
    /**
     * Maps this harvested text to the canonical localization to persist.
     *
     * The platform key is a [Country], the canonical key is its [Country.defaultLocale].
     *
     * @return the [AnimeLocalization] to pass to the persistence.
     * @see AnimeLocalization
     */
    fun toModel(): AnimeLocalization =
        AnimeLocalization(
            id = Uuid.random(),
            createdAt = TimeUtils.now(),
            updatedAt = TimeUtils.now(),
            locale = country.defaultLocale,
            title = title,
            description = description
        )
}
