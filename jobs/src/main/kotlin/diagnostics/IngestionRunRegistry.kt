package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.models.Platform
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicReference

/**
 * Keeps the last ingestion run of each platform, in memory only.
 *
 * A run is replaced on every record, so the registry never grows: it answers "what did the API
 * return last time, and why was each item dropped", not a history.
 */
@Single
class IngestionRunRegistry {
    private val runs = AtomicReference<Map<Platform, IngestionRun>>(emptyMap())

    fun record(run: IngestionRun) {
        runs.updateAndGet { it + (run.platform to run) }
    }

    fun latestRun(platform: Platform): IngestionRun? =
        runs.get()[platform]

    fun latestRuns(): List<IngestionRun> =
        runs.get().values.sortedBy { it.platform.name }
}
