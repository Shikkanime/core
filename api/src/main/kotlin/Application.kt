package fr.shikkanime.api

import com.expediagroup.graphql.server.operations.Query
import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.DatabaseModule
import fr.shikkanime.graphql.configureGraphQL
import fr.shikkanime.framework.koin.applyTransactionalProxies
import fr.shikkanime.ktor.ControllerBinder
import fr.shikkanime.ktor.configureSwaggerRoute
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.configureDefaultModules
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin

private val logger = LoggerFactory.getLogger("ApiApplication")

@Module
@ComponentScan("fr.shikkanime.api")
internal class AppModule

@KoinApplication(modules = [AppModule::class, DatabaseModule::class])
internal class MyApp

fun main() {
    logger.info("Starting API Application...")

    DatabaseManager.connect()

    val koin = startKoin<MyApp>().koin
    applyTransactionalProxies(koin)

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting API HTTP Server on port $port...")

    embeddedServer(CIO, port = port) {
        configureDefaultModules()

        configureGraphQL(
            schemaPackages = listOf("fr.shikkanime.api.graphql.queries", "fr.shikkanime.api.dtos"),
            schemaQueries = koin.getAll<Query>()
        )

        routing {
            configureSwaggerRoute("Shikkanime API", "1.0.0")

            ControllerBinder.register(
                this@routing,
                koin.getAll<IController>()
            )
        }
    }.start(wait = true)

    logger.info("API Application started successfully.")
}
