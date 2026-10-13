package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Country

/**
 * Editorial content localized for a specific language and region.
 *
 * @property country Eligible country where this localized content applies.
 * @property title Optional human-readable title or name of the episode.
 * @property description Optional summary or synopsis of the episode.
 */
data class PlatformEpisodeLocalization(
    val country: Country,
    val title: String?,
    val description: String?
)
