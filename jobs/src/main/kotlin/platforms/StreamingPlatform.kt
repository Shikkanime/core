package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Country
import fr.shikkanime.models.Platform

interface StreamingPlatform {
    val platform: Platform
    val countries: Set<Country>

    /**
     * Harvests the latest episodes published on this platform.
     *
     * @return the harvested episodes of every supported country, as produced by the platform.
     * @see [PlatformEpisode]
     */
    suspend fun fetchLatestEpisodes(): List<PlatformEpisode>
}
