package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.models.Platform
import org.koin.core.annotation.Single
import java.util.concurrent.atomic.AtomicReference

/**
 * Keeps the last ingestion run of each platform, in memory only.
 *
 * A run replaces the previous one on every [record], so the registry never grows: it answers
 * "what did the API return last time, and why was each item dropped", not a history.
 *
 * State is held as an immutable map behind an [AtomicReference] rather than a mutable one, so a
 * reader can never observe a half-updated snapshot.
 */
@Single
class IngestionRunRegistry {
    private val runs = AtomicReference<Map<Platform, IngestionRun>>(emptyMap())

    /**
     * Stores [run] as the latest run of its platform, replacing any previous one.
     */
    fun record(run: IngestionRun) {
        runs.updateAndGet { it + (run.platform to run) }
    }

    /**
     * Returns the last run recorded for [platform], or `null` if it never ran.
     */
    fun latestRun(platform: Platform): IngestionRun? =
        runs.get()[platform]

    /**
     * Returns the last run of every platform seen so far, ordered by platform name.
     */
    fun latestRuns(): List<IngestionRun> =
        runs.get().values.sortedBy { it.platform.name }
}
