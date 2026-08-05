package fr.shikkanime.site

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.ktor.configureDefaultModules
import io.ktor.server.engine.*
import io.ktor.server.cio.*

private val logger = LoggerFactory.getLogger("SiteApplication")

fun main() {
    logger.info("Starting Site Application...")
    val dbManager = DatabaseManager()
    dbManager.init()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8082
    logger.info("Starting Site HTTP Server on port $port...")

    embeddedServer(CIO, port = port) {
        configureDefaultModules()
    }.start(wait = false)

    logger.info("Site Application started successfully.")
}
