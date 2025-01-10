package fr.shikkanime.utils

import fr.shikkanime.jobs.AbstractJob
import fr.shikkanime.services.EmailService
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.time.ZonedDateTime
import java.util.logging.Level

object JobManager {
    data class JobError(
        val start: ZonedDateTime,
        var end: ZonedDateTime?,
        val stackTrace: String
    ) {
        override fun toString(): String {
            // Calculate the duration of the error
            val duration = end?.toEpochSecond()?.minus(start.toEpochSecond())
            val durationString = duration?.let { "${it / 3600}h ${it % 3600 / 60}m ${it % 60}s" } ?: "Now"
            return "- ${start.withUTCString()} - ${end?.withUTCString() ?: "Now"} ($durationString): $stackTrace"
        }
    }

    private val scheduler = StdSchedulerFactory().scheduler
    private val jobErrors = mutableMapOf<String, List<JobError>>()

    fun scheduleJob(cronExpression: String, vararg jobClasses: Class<out AbstractJob>) {
        jobClasses.forEach { jobClass ->
            val jobDetail = JobBuilder.newJob(JobExecutor::class.java)
                .withIdentity(jobClass.name)
                .build()

            val trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobClass.name)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build()

            scheduler.scheduleJob(jobDetail, trigger)
        }
    }

    fun start() {
        scheduler.start()
    }

    fun invalidate() {
        scheduler.clear()
    }

    class JobExecutor : Job {
        private val logger = LoggerFactory.getLogger(javaClass)

        override fun execute(context: JobExecutionContext?) {
            val jobName = context?.jobDetail?.key?.name ?: return
            val `class` = Class.forName(jobName) ?: return
            val job = Constant.injector.getInstance(`class`) as? AbstractJob ?: return
            val jobClassName = job.javaClass.simpleName
            val now = ZonedDateTime.now()

            try {
                job.run()

                if (jobErrors.containsKey(jobClassName)) {
                    val errors = jobErrors[jobClassName] ?: return
                    errors.forEach { it.end = now }
                    jobErrors.remove(jobClassName)
                    val emailService = Constant.injector.getInstance(EmailService::class.java)

                    emailService.sendAdminEmail(
                        "$jobClassName is now working",
                        """
                        The job was previously failing with the following errors:
                        ${errors.joinToString("\n") { it.toString() }}
                        """.trimIndent()
                    )

                    logger.info("$jobClassName is now working")
                    logger.info("The job was previously failing with the following errors:")
                    errors.forEach { logger.info(it.toString()) }
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while executing job $jobClassName", e)

                val stackTraceToString = e.stackTraceToString()
                val errors = jobErrors.getOrDefault(jobClassName, emptyList()).toMutableList()

                if (errors.none { it.stackTrace == stackTraceToString }) {
                    errors.add(JobError(now, null, stackTraceToString))
                    jobErrors[jobClassName] = errors
                    val emailService = Constant.injector.getInstance(EmailService::class.java)

                    emailService.sendAdminEmail(
                        "$jobClassName failed",
                        """
                        $jobClassName failed with the following error:
                        $stackTraceToString
                        """.trimIndent()
                    )
                }
            }
        }
    }
}