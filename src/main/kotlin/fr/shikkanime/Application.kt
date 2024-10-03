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
    // Every 20 seconds
    JobManager.scheduleJob("*/20 * * * * ?", FetchEpisodesJob::class.java)
    // Every 10 minutes
    JobManager.scheduleJob("0 */10 * * * ?", UpdateEpisodeJob::class.java)
    // Every hour
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    // Every day at midnight
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
    // Every day at 3pm
     JobManager.scheduleJob("0 0 15 * * ?", FetchOldEpisodesJob::class.java)
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
    val seasonRegex = " Saison (\\d)| ([MDCLXVI]+$)".toRegex()

    animeService.findAll().forEach {
        val removeAnimeNamePart = StringUtils.removeAnimeNamePart(it.name!!)

        if (removeAnimeNamePart != it.name) {
            val oldName = it.name
            it.name = removeAnimeNamePart
            logger.info("Updating name for anime $oldName to ${it.name}")
        }

        if (it.name!!.contains(seasonRegex)) {
            val seasonString = seasonRegex.find(it.name!!)!!.groupValues[0].trim()
            val season = seasonString.toIntOrNull() ?: StringUtils.romanToInt(seasonString)
            val oldName = it.name
            it.name = it.name!!.replace(seasonRegex, "")
            logger.info("Updating name for anime $oldName to ${it.name}")
            logger.warning("Replacing all season episodes for anime ${it.name} to season $season")

            episodeMappingService.findAllByAnime(it).forEach { episodeMapping ->
                episodeMapping.season = season
                episodeMappingService.update(episodeMapping)
            }
        }

        val toSlug = StringUtils.toSlug(StringUtils.getShortName(it.name!!))

        if (toSlug != it.slug) {
            it.slug = toSlug
            logger.info("Updating slug for anime ${it.name} to $toSlug")

            val existing = animeService.findBySlug(it.countryCode!!, toSlug)

            if (existing != null) {
                logger.warning("Slug $toSlug already exists, merging ${it.name} with ${existing.name}")
                animeService.merge(it, existing)
                return@forEach
            }
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
