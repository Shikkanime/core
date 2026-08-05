package fr.shikkanime.admin

import fr.shikkanime.admin.usecases.CreateAdminUserUseCase
import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.DatabaseModule
import fr.shikkanime.framework.koin.applyTransactionalProxies
import fr.shikkanime.ktor.ControllerBinder
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.configureDefaultModules
import fr.shikkanime.ktor.configureSwaggerRoute
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin

@Module
@ComponentScan("fr.shikkanime.admin")
class AppModule

@KoinApplication(modules = [AppModule::class, DatabaseModule::class])
class MyApp

private val logger = LoggerFactory.getLogger()

fun main() {
    logger.info("Starting Admin Application...")
    DatabaseManager.connect()

    val koin = startKoin<MyApp>().koin
    applyTransactionalProxies(koin)
    koin.get<CreateAdminUserUseCase>().execute()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    logger.info("Starting Admin HTTP Server on port $port...")

    embeddedServer(CIO, port = port) {
        configureDefaultModules()

        routing {
            route("/api") {
                configureSwaggerRoute("Admin API", "1.0-SNAPSHOT")
            }

            ControllerBinder.register(
                this@routing,
                koin.getAll<IController>()
            )
        }
    }.start(wait = true)
}
