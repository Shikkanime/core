package fr.shikkanime.jobs.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.Expression
import fr.shikkanime.jobs.metadata.MetadataLinker
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import java.util.logging.Level

/**
 * Links every stored anime and episode to its entries on the external metadata platforms
 * (AniList, MyAnimeList, AniDB).
 *
 * Runs weekly: the catalogs move slowly, and every request they answer is cached, so a fresh
 * pass only pays for what changed since the last one.
 *
 * Instantiated by the Quartz scheduler through Koin.
 */
@DisallowConcurrentExecution
@Single(binds = [Job::class])
@Expression("0 0 5 ? * MON")
internal class LinkMetadataJob(
    private val linker: MetadataLinker
) : Job {
    private val logger = LoggerFactory.getLogger()

    override fun execute(context: JobExecutionContext) {
        try {
            runBlocking {
                val report = linker.linkAll()
                logger.info(
                    "Metadata linking finished: ${report.animesProcessed} anime(s), " +
                        "${report.animeLinksSaved} anime link(s), " +
                        "${report.episodeLinksSaved} episode link(s), " +
                        "${report.unmatched.size} anime(s) with missing platform(s)"
                )
                report.unmatched.forEach { (title, platforms) ->
                    logger.warning("Unmatched metadata for '$title': $platforms")
                }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Metadata linking failed", e)
            throw JobExecutionException(e)
        }
    }
}
