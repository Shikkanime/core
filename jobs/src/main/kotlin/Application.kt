package fr.shikkanime.jobs

import org.koin.core.Koin
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import kotlin.reflect.full.findAnnotation

@Module
@ComponentScan("fr.shikkanime.jobs")
internal class AppModule

@KoinApplication(modules = [AppModule::class])
internal class MyApp

internal class KoinJobFactory(private val koin: Koin) : JobFactory {
    override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job =
        koin.get(bundle.jobDetail.jobClass.kotlin)
}

fun main() {
    val koin = startKoin<MyApp>().koin

    StdSchedulerFactory().scheduler
        .apply {
            setJobFactory(KoinJobFactory(koin))
            schedule(koin.getAll<Job>())
        }.start()
}

private fun Scheduler.schedule(jobs: List<Job>) {
    jobs.forEach { job ->
        val expression = job::class.findAnnotation<Expression>()?.value
            ?: return@forEach

        val detail = JobBuilder.newJob(job::class.java).build()
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(expression)
                    .withMisfireHandlingInstructionDoNothing()
            ).build()

        scheduleJob(detail, trigger)
    }
}