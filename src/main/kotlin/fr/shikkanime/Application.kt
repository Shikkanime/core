package fr.shikkanime

import fr.shikkanime.jobs.FetchEpisodesJob
import fr.shikkanime.jobs.GCJob
import fr.shikkanime.jobs.MetricJob
import fr.shikkanime.plugins.configureHTTP
import fr.shikkanime.plugins.configureRouting
import fr.shikkanime.plugins.configureSecurity
import fr.shikkanime.utils.JobManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    JobManager.scheduleJob("*/10 * * * * ?", MetricJob::class.java)
    JobManager.scheduleJob("0 */5 * * * ?", GCJob::class.java)
    JobManager.scheduleJob("0 * * * * ?", FetchEpisodesJob::class.java)
    JobManager.start()

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
