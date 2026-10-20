package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Country

/**
 * Editorial content localized for a specific language and region.
 *
 */
data class PlatformEpisodeLocalization(
    val country: Country,
    val title: String?,
    val description: String?
)
