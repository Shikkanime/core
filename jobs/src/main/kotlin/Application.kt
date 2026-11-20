package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.DatabaseModule
import fr.shikkanime.jobs.services.HarvestIngestionService
import fr.shikkanime.framework.koin.applyTransactionalProxies
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.core.Koin
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import java.time.ZonedDateTime
import kotlin.reflect.full.findAnnotation

private val logger = LoggerFactory.getLogger("Application")

/**
 * Boots the application graph shared by the entrypoint and the integration tests.
 *
 * @return the started Koin instance, with its transactional proxies applied.
 * @see [startKoin]
 * @see [applyTransactionalProxies]
 */
internal fun bootstrap(): Koin {
    val koin = startKoin<MyApp>().koin
    applyTransactionalProxies(koin)
    return koin
}

@Module
@ComponentScan("fr.shikkanime.jobs")
internal class AppModule

@KoinApplication(modules = [AppModule::class, DatabaseModule::class])
internal class MyApp

internal class KoinJobFactory(private val koin: Koin) : JobFactory {
    override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job =
        koin.get(bundle.jobDetail.jobClass.kotlin)
}

class ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val string = decoder.decodeString()
        return ZonedDateTime.parse(string)
    }
}

/**
 * Starts the jobs scheduler of the application.
 *
 * Boots the application graph with its transactional proxies and connects to the database, then
 * registers every [Job] bean on a Quartz scheduler through its declared [Expression] cron
 * schedule. Each job instance is resolved from Koin by the [KoinJobFactory] when its trigger
 * fires, and the process stays alive until it is stopped.
 *
 * @return nothing; the scheduler runs until the process is stopped.
 * @see [Job]
 * @see [Expression]
 * @see [KoinJobFactory]
 * @see [HarvestIngestionService]
 */
suspend fun main() {
    val koin = bootstrap()

    DatabaseManager.connect()

    val jobs = koin.getAll<Job>()
    val scheduler = StdSchedulerFactory().scheduler
    scheduler.setJobFactory(KoinJobFactory(koin))
    val scheduled = scheduler.schedule(jobs)
    scheduler.start()
    logger.info("Started the jobs scheduler with $scheduled job(s)")

    awaitCancellation()
}

/**
 * Registers every given job on this scheduler with its declared cron expression.
 *
 * A job carrying no [Expression] annotation defines no schedule and is skipped.
 *
 * @receiver scheduler the jobs are registered on.
 * @param jobs jobs to schedule.
 * @return the number of jobs actually registered on this scheduler.
 * @see [Expression]
 * @see [TriggerBuilder]
 */
internal fun Scheduler.schedule(jobs: List<Job>): Int {
    var scheduled = 0
    jobs.forEach { job ->
        val expression = job::class.findAnnotation<Expression>()?.value
        if (expression == null) {
            logger.warning("Skipping job ${job::class.simpleName}: no @Expression declared")
            return@forEach
        }

        val detail = JobBuilder.newJob(job::class.java)
            .build()
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(expression)
                    .withMisfireHandlingInstructionDoNothing()
            ).build()

        scheduleJob(detail, trigger)
        scheduled++
    }
    return scheduled
}