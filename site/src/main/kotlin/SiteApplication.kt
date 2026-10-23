package fr.shikkanime.site

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.ktor.configureDefaultModules
import io.ktor.server.engine.*
import io.ktor.server.cio.*

private val logger = LoggerFactory.getLogger("SiteApplication")

fun main() {
    logger.info("Starting Site Application...")
    DatabaseManager.connect()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8082
    logger.info("Starting Site HTTP Server on port $port...")

    // The process serves until it is stopped, like the other applications: returning here would
    // end the JVM as soon as the server starts.
    embeddedServer(CIO, port = port) {
        configureDefaultModules()
    }.start(wait = true)
}
