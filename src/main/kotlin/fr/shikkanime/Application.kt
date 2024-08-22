package fr.shikkanime

import fr.shikkanime.jobs.*
import fr.shikkanime.modules.configureHTTP
import fr.shikkanime.modules.configureRouting
import fr.shikkanime.modules.configureSecurity
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberService
import fr.shikkanime.socialnetworks.DiscordSocialNetwork
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

private val logger = LoggerFactory.getLogger(Constant.NAME)

fun main() {
    logger.info("Starting ${Constant.NAME}...")
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    animeService.preIndex()
    updateAndDeleteData(animeService)

    ImageService.loadCache()
    ImageService.addAll()

    try {
        Constant.injector.getInstance(MemberService::class.java).initDefaultAdminUser()
    } catch (e: IllegalStateException) {
        logger.info("Admin user already exists")
    }

    logger.info("Starting jobs...")
    // Every 10 seconds
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    // Every minute
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    // Every 10 minutes
    JobManager.scheduleJob("0 */10 * * * ?", UpdateEpisodeJob::class.java)
    // Every hour
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    // Every day at midnight
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
    // Every day at 3pm
     JobManager.scheduleJob("0 0 15 * * ?", FetchOldEpisodesJob::class.java)
    // Every day at 9am
    JobManager.scheduleJob("0 0 9 * * ?", FetchCalendarJob::class.java)
    JobManager.start()

    Constant.injector.getInstance(DiscordSocialNetwork::class.java).login()

    logger.info("Starting server...")
    embeddedServer(
        Netty,
        port = Constant.PORT,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

private fun updateAndDeleteData(animeService: AnimeService) {
    val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)
    val seasonRegex = " Saison (\\d)".toRegex()

    animeService.findAll().forEach {
        if (it.name!!.contains(seasonRegex)) {
            val season = seasonRegex.find(it.name!!)!!.groupValues[1]
            it.name = it.name!!.replace(seasonRegex, "")
            logger.info("Updating name for anime ${it.name} to ${it.name}")
            logger.warning("Replacing all season episodes for anime ${it.name} to season $season")

            episodeMappingService.findAllByAnime(it).forEach { episodeMapping ->
                episodeMapping.season = season.toInt()
                episodeMappingService.update(episodeMapping)
            }
        }

        val toSlug = StringUtils.toSlug(StringUtils.getShortName(it.name!!))

        if (toSlug != it.slug) {
            it.slug = toSlug
            logger.info("Updating slug for anime ${it.name} to $toSlug")
        }

        it.status = StringUtils.getStatus(it)
        animeService.update(it)
    }
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureRouting()
}
