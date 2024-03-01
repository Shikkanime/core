package fr.shikkanime

import fr.shikkanime.jobs.*
import fr.shikkanime.modules.configureHTTP
import fr.shikkanime.modules.configureRouting
import fr.shikkanime.modules.configureSecurity
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.LoggerFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger(Constant.NAME)

fun main() {
    logger.info("Starting ${Constant.NAME}...")
    initAll(AtomicReference())
}

fun initAll(adminPassword: AtomicReference<String>?, port: Int = 37100, wait: Boolean = true): NettyApplicationEngine {
    ImageService.loadCache()

    if (adminPassword != null) {
        val memberService = Constant.injector.getInstance(MemberService::class.java)

        try {
            adminPassword.set(memberService.initDefaultAdminUser())
        } catch (e: IllegalStateException) {
            logger.info("Admin user already exists")
        }
    }

    Constant.injector.getInstance(AnimeService::class.java).preIndex()
    ImageService.addAll()

    logger.info("Starting jobs...")
    // Every 10 seconds
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    // Every minute
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    // Every hour
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    JobManager.scheduleJob("0 0 * * * ?", FetchDeprecatedEpisodeJob::class.java)
    // Every day at midnight
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
    JobManager.start()

    logger.info("Starting server...")
    return embeddedServer(
        Netty,
        port = port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = wait)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureRouting()
}
