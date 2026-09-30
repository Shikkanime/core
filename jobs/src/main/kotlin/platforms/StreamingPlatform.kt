package fr.shikkanime.jobs.platforms

import fr.shikkanime.jobs.diagnostics.IngestionRun
import fr.shikkanime.models.Platform

/**
 * A streaming platform the jobs module can ingest episodes from.
 */
interface StreamingPlatform {
    /** Platform this implementation reads from. */
    val platform: Platform

    /**
     * Returns the episodes ready to be ingested right now.
     */
    suspend fun fetchLatestEpisodes(): List<PlatformEpisode>

    /**
     * Describes the last fetch without any filtering side effect: one verdict per item the
     * platform returned, so a rejected episode can be traced to the rule that dropped it.
     */
    suspend fun diagnoseLatestEpisodes(): IngestionRun
}
