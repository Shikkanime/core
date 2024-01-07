package fr.shikkanime.utils

import fr.shikkanime.jobs.AbstractJob
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.logging.Level

object JobManager {
    private val scheduler = StdSchedulerFactory().scheduler

    fun scheduleJob(cronExpression: String, jobClass: Class<out AbstractJob>) {
        val jobDetail = JobBuilder.newJob(JobExecutor::class.java)
            .withIdentity(jobClass.name)
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity(jobClass.name)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }

    fun start() {
        scheduler.start()
    }

    class JobExecutor : Job {
        private val logger = LoggerFactory.getLogger(javaClass)

        override fun execute(context: JobExecutionContext?) {
            val jobName = context?.jobDetail?.key?.name ?: return
            val `class` = Class.forName(jobName) ?: return
            val job = Constant.injector.getInstance(`class`) as? AbstractJob ?: return

            try {
                job.run()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while executing job $jobName", e)
            }
        }
    }
}