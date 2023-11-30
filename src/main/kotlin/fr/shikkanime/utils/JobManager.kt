package fr.shikkanime.utils

import fr.shikkanime.jobs.AbstractJob
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

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
        override fun execute(context: JobExecutionContext?) {
            val jobName = context?.jobDetail?.key?.name ?: return
            val `class` = Class.forName(jobName) ?: return
            val job = Constant.guice.getInstance(`class`) as? AbstractJob ?: return
            job.run()
        }
    }
}