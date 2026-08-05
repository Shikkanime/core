package fr.shikkanime.api

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.ktor.configureDefaultModules
import io.ktor.server.engine.*
import io.ktor.server.cio.*

private val logger = LoggerFactory.getLogger("ApiApplication")

fun main() {
    logger.info("Starting API Application...")
    val dbManager = DatabaseManager()
    dbManager.init()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting API HTTP Server on port $port...")

    embeddedServer(CIO, port = port) {
        configureDefaultModules()
    }.start(wait = false)

    logger.info("API Application started successfully.")
}
