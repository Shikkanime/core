package fr.shikkanime

import fr.shikkanime.jobs.*
import fr.shikkanime.plugins.configureHTTP
import fr.shikkanime.plugins.configureRouting
import fr.shikkanime.plugins.configureSecurity
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

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

    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    animeService.preIndex()

    animeService.findAll()
        .filter { it.slug.isNullOrBlank() || it.slug != StringUtils.toSlug(StringUtils.getShortName(it.name!!)) }
        .forEach {
            val name = StringUtils.getShortName(it.name!!)
            val slug = StringUtils.toSlug(name)
            it.slug = slug
            animeService.update(it)
        }

    ImageService.addAll()

    logger.info("Starting jobs...")
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    JobManager.scheduleJob("0 */10 * * * ?", GarbageCollectorJob::class.java)
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
    JobManager.start()

    logger.info("Starting server...")
    embeddedServer(
        CIO,
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
