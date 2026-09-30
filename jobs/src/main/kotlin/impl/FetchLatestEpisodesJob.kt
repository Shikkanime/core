package fr.shikkanime.jobs.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.Expression
import fr.shikkanime.jobs.diagnostics.IngestionRun
import fr.shikkanime.jobs.diagnostics.IngestionRunRegistry
import fr.shikkanime.jobs.platforms.StreamingPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.time.Clock

@DisallowConcurrentExecution
@Single(binds = [Job::class])
@Expression("*/20 * * * * ?")
class FetchLatestEpisodesJob(
    private val platforms: List<StreamingPlatform>,
    private val registry: IngestionRunRegistry
) : Job {
    private val logger = LoggerFactory.getLogger()

    /**
     * Diagnoses every platform, then records and logs one report per platform.
     *
     * A platform that throws is recorded as a failed run rather than skipped: leaving the
     * previous run in place would show a stale result as if it were fresh, which is exactly the
     * kind of silent loss this diagnosis is meant to rule out.
     */
    override fun execute(context: JobExecutionContext) = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        platforms.forEach { platform ->
            val run = try {
                platform.diagnoseLatestEpisodes()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                IngestionRun(
                    platform = platform.platform,
                    verdicts = emptyList(),
                    fetchedAt = now,
                    error = "${exception::class.simpleName}: ${exception.message}"
                )
            }

            registry.record(run)
            logger.info(run.describe())
        }
    }
}
