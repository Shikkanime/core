package fr.shikkanime

import fr.shikkanime.jobs.*
import fr.shikkanime.modules.configureHTTP
import fr.shikkanime.modules.configureRouting
import fr.shikkanime.modules.configureSecurity
import fr.shikkanime.services.*
import fr.shikkanime.services.admin.AnimeAdminService
import fr.shikkanime.utils.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.logging.Level
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger(Constant.NAME)

fun main(args: Array<String>) {
    logger.info("Starting ${Constant.NAME}...")

    logger.info("Testing Playwright installation...")
    checkPlaywrightInstallation()

    logger.info("Loading attachments cache...")
    val attachmentService = Constant.injector.getInstance(AttachmentService::class.java)
    attachmentService.encodeAllActiveWithUrlAndWithoutFile()

    logger.info("Updating and deleting data...")
    updateAndDeleteData()

    logger.info("Pre-indexing anime data...")
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)
    animeService.preIndex()
    episodeVariantService.preIndex()

    try {
        val memberService = Constant.injector.getInstance(MemberService::class.java)
        memberService.initDefaultAdminUser()
    } catch (_: IllegalStateException) {
        logger.info("Admin user already exists")
    }

    if ("--enable-jobs" in args) {
        logger.info("Starting jobs...")
        // Every 10 seconds
        JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
        // Every 20 seconds
        JobManager.scheduleJob("*/20 * * * * ?", FetchEpisodesJob::class.java, SendMailJob::class.java)
        // Every 10 minutes
        JobManager.scheduleJob("0 */10 * * * ?", UpdateEpisodeMappingJob::class.java, UpdateAttachmentJob::class.java)
        // Every hour
        JobManager.scheduleJob("0 0 * * * ?", UpdateAnimeJob::class.java)
        // Every day at midnight
        JobManager.scheduleJob("0 0 0 * * ?", DailyJob::class.java)
        JobManager.start()
    } else {
        logger.warning("Jobs are disabled, use --enable-jobs to enable them")
    }

    logger.info("Starting server...")
    embeddedServer(
        Netty,
        port = Constant.PORT,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

private fun checkPlaywrightInstallation() {
    try {
        HttpRequest().use {
            it.getWithBrowser("https://playwright.dev")
            logger.info("Playwright is installed correctly.")
        }
    } catch (e: Exception) {
        logger.log(Level.SEVERE, "Playwright installation failed", e)
        exitProcess(1)
    }
}

private fun updateAndDeleteData() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val animeAdminService = Constant.injector.getInstance(AnimeAdminService::class.java)
    val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)
    val seasonRegex = " Saison (\\d)| ([${StringUtils.ROMAN_NUMBERS_CHECK}]+$)".toRegex()
    val animes = animeService.findAll()

    animes.forEach { anime ->
        val oldName = anime.name
        anime.name = StringUtils.removeAnimeNamePart(anime.name!!)

        if (oldName != anime.name) {
            logger.info("Updating name for anime $oldName to ${anime.name}")
        }

        if (seasonRegex in anime.name!!) {
            val seasonString = seasonRegex.find(anime.name!!)!!.groupValues[0].trim()
            val season = seasonString.toIntOrNull() ?: StringUtils.romanToInt(seasonString)
            anime.name = anime.name!!.replace(seasonRegex, StringUtils.EMPTY_STRING)
            logger.info("Updating name for anime $oldName to ${anime.name}")
            logger.warning("Replacing all season episodes for anime ${anime.name} to season $season")

            episodeMappingService.findAllByAnime(anime)
                .forEach { episodeMapping ->
                    episodeMapping.season = season
                    episodeMappingService.update(episodeMapping)
                }
        }

        val slug = StringUtils.toSlug(StringUtils.getShortName(anime.name!!))

        if (slug != anime.slug) {
            anime.slug = slug
            logger.info("Updating slug for anime ${anime.name} to $slug")

            animeService.findBySlug(anime.countryCode!!, slug)?.let { existing ->
                logger.warning("Slug $slug already exists, merging ${anime.name} with ${existing.name}")
                animeAdminService.merge(anime, existing)
                return@forEach
            }
        }
    }

    animeService.updateAll(animes)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureRouting()
}
