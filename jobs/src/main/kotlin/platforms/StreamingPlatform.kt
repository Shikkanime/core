package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Platform

interface StreamingPlatform {
    val platform: Platform

    suspend fun fetchEpisodes(): List<PlatformEpisode>
}
