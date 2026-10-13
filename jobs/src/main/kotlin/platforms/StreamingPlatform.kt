package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Country
import fr.shikkanime.models.Platform

interface StreamingPlatform {
    val platform: Platform
    val countries: Set<Country>

    suspend fun fetchLatestEpisodes(): List<PlatformEpisode>
}
