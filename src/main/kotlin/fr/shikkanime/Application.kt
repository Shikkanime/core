package fr.shikkanime

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.jobs.*
import fr.shikkanime.modules.configureHTTP
import fr.shikkanime.modules.configureRouting
import fr.shikkanime.modules.configureSecurity
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
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
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)
    animeService.preIndex()

    updateAndDeleteData(episodeMappingService, animeService)

    ImageService.loadCache()
    ImageService.addAll()

    if (adminPassword != null) {
        val memberService = Constant.injector.getInstance(MemberService::class.java)

        try {
            adminPassword.set(memberService.initDefaultAdminUser())
        } catch (e: IllegalStateException) {
            logger.info("Admin user already exists")
        }
    }

    logger.info("Starting jobs...")
    // Every 10 seconds
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    // Every minute
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    // Every 10 minutes
    JobManager.scheduleJob("0 */10 * * * ?", GarbageCollectorJob::class.java)
    // Every hour
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    // Every day at midnight
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
    JobManager.scheduleJob("0 0 15 * * ?", FetchOldEpisodesJob::class.java)
    // Every day at 9am
    JobManager.scheduleJob("0 0 9 * * ?", FetchCalendarJob::class.java)
    JobManager.start()

    logger.info("Starting server...")
    return embeddedServer(
        Netty,
        port = port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = wait)
}

private fun updateAndDeleteData(episodeMappingService: EpisodeMappingService, animeService: AnimeService) {
    episodeMappingService.findAllByEpisodeType(EpisodeType.FILM).forEach {
        episodeMappingService.delete(it)
    }

    animeService.findAll().forEach {
        val mappings = episodeMappingService.findAllByAnime(it)

        if (mappings.isEmpty()) {
            logger.info("Deleting anime ${StringUtils.getShortName(it.name!!)} because it has no episodes")
            animeService.delete(it)
            return@forEach
        }

        it.status = StringUtils.getStatus(it)
        animeService.update(it)
    }

    episodeMappingService.findAll().forEach {
        it.status = StringUtils.getStatus(it)
        episodeMappingService.update(it)
    }
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureRouting()
}
