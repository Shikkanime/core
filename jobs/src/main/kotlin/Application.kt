package fr.shikkanime.jobs

import fr.shikkanime.jobs.platforms.StreamingPlatform
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
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import java.time.ZonedDateTime
import kotlin.reflect.full.findAnnotation
import kotlin.system.exitProcess

@Module
@ComponentScan("fr.shikkanime.jobs")
internal class AppModule

@KoinApplication(modules = [AppModule::class])
internal class MyApp

internal class KoinJobFactory(private val koin: Koin) : JobFactory {
    override fun newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job =
        koin.get(bundle.jobDetail.jobClass.kotlin)
}

class ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val string = decoder.decodeString()
        return ZonedDateTime.parse(string)
    }
}

suspend fun main() {
    val koin = startKoin<MyApp>().koin

    koin.getAll<StreamingPlatform>().forEach { streamingPlatform ->
        println(streamingPlatform.fetchLatestEpisodes())
    }

    exitProcess(0)

//    StdSchedulerFactory().scheduler
//        .apply {
//            setJobFactory(KoinJobFactory(koin))
//            schedule(koin.getAll<Job>())
//        }.start()
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