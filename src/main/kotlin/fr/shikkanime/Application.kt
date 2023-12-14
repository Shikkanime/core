package fr.shikkanime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.jais.EpisodeDto
import fr.shikkanime.entities.Episode
import fr.shikkanime.jobs.*
import fr.shikkanime.plugins.configureHTTP
import fr.shikkanime.plugins.configureRouting
import fr.shikkanime.plugins.configureSecurity
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking

fun main() {
    println("Starting ShikkAnime...")
    ImageService.loadCache()

    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeService = Constant.injector.getInstance(EpisodeService::class.java)

    animeService.preIndex()
    animeService.findAll().forEach { ImageService.add(it.uuid!!, it.image!!, 480, 720) }
    episodeService.findAll().forEach { ImageService.add(it.uuid!!, it.image!!, 640, 360) }

    Constant.injector.getInstance(MemberService::class.java).initDefaultAdminUser()

    runBlocking {
        val httpResponse = HttpRequest().get("https://beta-api.ziedelth.fr/episodes/country/fr/page/1/limit/30")

        if (httpResponse.status.isSuccess()) {
            val episodes =
                AbstractConverter.convert(ObjectParser.fromJson(httpResponse.bodyAsText(), Array<EpisodeDto>::class.java).toList(), Episode::class.java)
            episodes.filter { it.uuid == null }.forEach { episodeService.save(it) }
        }
    }

    println("Starting jobs...")
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    JobManager.scheduleJob("0 */5 * * * ?", GCJob::class.java)
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    JobManager.scheduleJob("0 */5 * * * ?", SavingImageCacheJob::class.java)
    JobManager.scheduleJob("0 */15 * * * ?", BackupJob::class.java)
    JobManager.start()

    println("Starting server...")
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
