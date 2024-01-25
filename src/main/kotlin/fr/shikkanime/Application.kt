package fr.shikkanime

import fr.shikkanime.jobs.*
import fr.shikkanime.plugins.configureHTTP
import fr.shikkanime.plugins.configureRouting
import fr.shikkanime.plugins.configureSecurity
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.LoggerFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.UUID

private val logger = LoggerFactory.getLogger("Shikkanime")

fun main() {
    logger.info("Starting ShikkAnime...")
    ImageService.loadCache()

    val memberService = Constant.injector.getInstance(MemberService::class.java)
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeService = Constant.injector.getInstance(EpisodeService::class.java)

    try {
        memberService.initDefaultAdminUser()
    } catch (e: IllegalStateException) {
        logger.info("Admin user already exists")
    }

    animeService.preIndex()
    animeService.findAllUUIDAndImage().forEach { animeService.addImage(it[0] as UUID, it[1] as String) }
    episodeService.findAllUUIDAndImage().forEach { episodeService.addImage(it[0] as UUID, it[1] as String) }

    // Sync episodes from Jais
//    if (false) {
//        val episodes = mutableListOf<Episode>()
//
//        (150 downTo 1).forEach {
//            runBlocking {
//                val httpResponse =
//                    HttpRequest().get("https://beta-api.ziedelth.fr/episodes/country/fr/page/$it/limit/30")
//
//                if (httpResponse.status.isSuccess()) {
//                    episodes.addAll(
//                        AbstractConverter.convert(
//                            ObjectParser.fromJson(httpResponse.bodyAsText(), Array<EpisodeDto>::class.java).toList(),
//                            Episode::class.java
//                        )
//                    )
//                }
//            }
//        }
//
//        episodes.filter { it.uuid == null || it.platform?.name != "Disney+" }.forEach { episodeService.save(it) }
//    }

    logger.info("Starting jobs...")
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    JobManager.scheduleJob("0 0 * * * ?", SavingImageCacheJob::class.java)
    JobManager.scheduleJob("0 */10 * * * ?", GarbageCollectorJob::class.java)
    JobManager.scheduleJob("0 0 0 * * ?", DeleteOldMetricsJob::class.java)
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
