package fr.shikkanime.jobs.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.jobs.Expression
import fr.shikkanime.jobs.platforms.StreamingPlatform
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDateTime
import org.koin.core.annotation.Single
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.time.ZonedDateTime

@DisallowConcurrentExecution
@Single(binds = [Job::class])
@Expression("*/20 * * * * ?")
class FetchLatestEpisodesJob(
    private val streamingPlatforms: List<StreamingPlatform>,
    private val episodeService: EpisodeService
) : Job {
    private val logger = LoggerFactory.getLogger()

    override fun execute(context: JobExecutionContext) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)

        streamingPlatforms.forEach { streamingPlatform ->
            val platform = streamingPlatform.platform

            try {
                // Platform returns ALL its episodes; caching keeps this light between changes
                val episodes = runBlocking { streamingPlatform.fetchEpisodes() }

                // Keep only episodes whose release time has passed
                val released = episodes.filter { it.releaseDateTime <= now }

                if (released.isEmpty()) return@forEach

                // Single batch query per platform: only unknown episodes will be converted and saved
                val existingIds = episodeService.findAllExistingPlatformEpisodeIds(
                    platform,
                    released.map { it.id }
                )

                released
                    .filter { it.id !in existingIds }
                    .forEach { episode ->
                        // Persist in UTC so release times stay comparable across platforms
                        episodeService.save(
                            platform,
                            episode.id,
                            episode.title,
                            episode.releaseDateTime
                                .withZoneSameInstant(ZoneOffset.UTC)
                                .toLocalDateTime()
                                .toKotlinLocalDateTime()
                        )
                    }
            } catch (e: Exception) {
                logger.severe("Error while processing platform $platform: ${e.message}")
            }
        }
    }
}
