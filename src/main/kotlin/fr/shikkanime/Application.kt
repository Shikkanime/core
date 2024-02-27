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

private val logger = LoggerFactory.getLogger(Constant.NAME)

fun main() {
    logger.info("Starting ${Constant.NAME}...")
    ImageService.loadCache()

    val memberService = Constant.injector.getInstance(MemberService::class.java)

    try {
        memberService.initDefaultAdminUser()
    } catch (e: IllegalStateException) {
        logger.info("Admin user already exists")
    }

    Constant.injector.getInstance(AnimeService::class.java).preIndex()
    ImageService.addAll()

    logger.info("Starting jobs...")
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    JobManager.scheduleJob("0 */10 * * * ?", GarbageCollectorJob::class.java)
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
    JobManager.scheduleJob("0 0 * * * ?", FetchDeprecatedEpisodeJob::class.java)
    JobManager.start()

    logger.info("Starting server...")
    embeddedServer(
        Netty,
        port = 37100,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureRouting()
}
