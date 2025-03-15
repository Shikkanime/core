package fr.shikkanime

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

private val logger = LoggerFactory.getLogger(Constant.NAME)

fun main(args: Array<String>) {
    logger.info("Starting ${Constant.NAME}...")

    logger.info("Pre-indexing anime data...")
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val memberService = Constant.injector.getInstance(MemberService::class.java)
    animeService.preIndex()

    logger.info("Loading images cache...")
    ImageService.loadCache()
    ImageService.addAll()

    logger.info("Updating and deleting data...")
    updateAndDeleteData()

    try {
        memberService.initDefaultAdminUser()
    } catch (_: IllegalStateException) {
        logger.info("Admin user already exists")
    }

    if (args.contains("--enable-jobs")) {
        logger.info("Starting jobs...")
        // Every 10 seconds
        JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
        // Every 20 seconds
        JobManager.scheduleJob("*/20 * * * * ?", FetchEpisodesJob::class.java, SendMailJob::class.java)
        // Every 10 minutes
        JobManager.scheduleJob("0 */10 * * * ?", UpdateEpisodeMappingJob::class.java, UpdateImageJob::class.java)
        // Every hour
        JobManager.scheduleJob("0 0 * * * ?", UpdateAnimeJob::class.java)
        // Every day at midnight
        JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
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

private fun updateAndDeleteData() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)
    val seasonRegex = " Saison (\\d)| ([${StringUtils.ROMAN_NUMBERS_CHECK}]+$)".toRegex()
    val animes = animeService.findAll()

    animes.forEach { anime ->
        val oldName = anime.name
        anime.name = StringUtils.removeAnimeNamePart(anime.name!!)

        if (oldName != anime.name) {
            logger.info("Updating name for anime $oldName to ${anime.name}")
        }

        if (anime.name!!.contains(seasonRegex)) {
            val seasonString = seasonRegex.find(anime.name!!)!!.groupValues[0].trim()
            val season = seasonString.toIntOrNull() ?: StringUtils.romanToInt(seasonString)
            anime.name = anime.name!!.replace(seasonRegex, "")
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
                animeService.merge(anime, existing)
                return@forEach
            }
        }

        anime.status = StringUtils.getStatus(anime)
    }

    animeService.updateAll(animes)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureRouting()
}
