package fr.shikkanime.jobs.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.Expression
import fr.shikkanime.jobs.SmartResponse
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.jobs.services.HarvestIngestionService
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import java.util.logging.Level

/**
 * Harvests the latest episodes of every streaming platform and persists them.
 *
 * Every platform is fetched first, then one global pass where the episode of a normalized
 * title is attributed to the platform with the highest source priority, so a lower priority
 * platform never overwrites the data of a higher one. The save workflow only runs when at
 * least one platform reports a change in its harvest.
 *
 * Instantiated by the Quartz scheduler through Koin.
 */
@DisallowConcurrentExecution
@Single(binds = [Job::class])
@Expression("*/20 * * * * ?")
internal class FetchLatestEpisodesJob(
    private val platforms: List<StreamingPlatform>,
    private val ingestionService: HarvestIngestionService
) : Job {
    private val logger = LoggerFactory.getLogger()

    override fun execute(context: JobExecutionContext) {
        try {
            runBlocking {
                val harvest = platforms.map { it.fetchLatestEpisodes() }
                val changed = harvest.count(SmartResponse<*>::hasChanged)
                val saved = ingestionService.saveAll(harvest)
                logger.info(
                    "Harvest finished: ${harvest.size} platform(s), " +
                        "$changed changed, $saved animes saved"
                )
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Harvest failed", e)
            throw JobExecutionException(e)
        }
    }
}
