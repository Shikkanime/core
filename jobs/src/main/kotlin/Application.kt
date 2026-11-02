package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.DatabaseModule

import fr.shikkanime.jobs.platforms.StreamingPlatform
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import fr.shikkanime.framework.koin.applyTransactionalProxies
import org.quartz.*
import org.koin.core.Koin
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin

import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import java.time.ZonedDateTime
import kotlin.reflect.full.findAnnotation
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Application")

@Module
@ComponentScan("fr.shikkanime.jobs")
internal class AppModule

@KoinApplication(modules = [AppModule::class, DatabaseModule::class])
internal class MyApp

@Suppress("unused")
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
 * Runs one full harvest pass of every streaming platform.
 *
 * Boots the application graph with its transactional proxies and connects to the database, harvests
 * the latest episodes of every platform, then persists the animes they carry before exiting the
 * process.
 *
 * @return nothing; the process exits once the harvest is persisted.
 * @see [StreamingPlatform]
 * @see [HarvestIngestionService]
 */
suspend fun main() {
    val koin = startKoin<MyApp>().koin
    applyTransactionalProxies(koin)

    DatabaseManager.connect()

    // Every platform is harvested first, then one global pass: the platforms are sorted by their
    // source priority and the first anime of a given normalized title wins, so a lower priority
    // platform never overwrites the data of a higher one.
    val harvest = koin.getAll<StreamingPlatform>()
        // The lambda is required: a suspend function reference is not a valid map candidate
        .map { it.fetchLatestEpisodes() }

    // Persist the pooled harvest before the process exits
    val saved = koin.get<HarvestIngestionService>()
        .saveAll(harvest)
    logger.info("Saved $saved animes")

    exitProcess(0)

//    StdSchedulerFactory().scheduler
//        .apply {
//            setJobFactory(KoinJobFactory(koin))
//            schedule(koin.getAll<Job>())
//        }.start()
}

/**
 * Registers every given job on this scheduler with its declared cron expression.
 *
 * A job carrying no [Expression] annotation defines no schedule and is skipped.
 *
 * @receiver scheduler the jobs are registered on.
 * @param jobs jobs to schedule.
 * @return nothing; the jobs are registered with their own trigger.
 * @see [Expression]
 * @see [TriggerBuilder]
 */
@Suppress("unused")
private fun Scheduler.schedule(jobs: List<Job>) {
    jobs.forEach { job ->
        val expression = job::class.findAnnotation<Expression>()?.value
            ?: return@forEach

        val detail = JobBuilder.newJob(job::class.java)
            .build()
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(expression)
                    .withMisfireHandlingInstructionDoNothing()
            ).build()

        scheduleJob(detail, trigger)
    }
}