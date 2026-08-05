package fr.shikkanime.jobs.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.Expression
import org.koin.core.annotation.Single
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext

@DisallowConcurrentExecution
@Single(binds = [Job::class])
@Expression("*/20 * * * * ?")
class FetchLatestEpisodesJob : Job {
    private val logger = LoggerFactory.getLogger()

    override fun execute(context: JobExecutionContext) {
        TODO("Not yet implemented")
    }
}