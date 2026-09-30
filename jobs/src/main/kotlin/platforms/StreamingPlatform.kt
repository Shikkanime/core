package fr.shikkanime.jobs.platforms

import fr.shikkanime.jobs.diagnostics.IngestionRun
import fr.shikkanime.models.Platform

interface StreamingPlatform {
    val platform: Platform

    suspend fun fetchLatestEpisodes(): List<PlatformEpisode>

    /**
     * Describes the last fetch without any filtering side effect: one verdict per item the
     * platform returned, so a rejected episode can be traced to the rule that dropped it.
     */
    suspend fun diagnoseLatestEpisodes(): IngestionRun
}
