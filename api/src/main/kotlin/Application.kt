package fr.shikkanime.api

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.DatabaseModule
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

@Module
@ComponentScan("fr.shikkanime.api")
class AppModule

@KoinApplication(modules = [AppModule::class, DatabaseModule::class])
class MyApp

/**
 * Starts the Shikkanime REST API.
 *
 * Connects the database, boots the application graph with its transactional proxies, then serves
 * the API on the `PORT` environment variable (8080 when unset) until the process is stopped.
 *
 * @return nothing; the embedded server blocks the calling thread until it is stopped.
 * @see [MyApp]
 * @see [DatabaseManager]
 */
fun main() {
    DatabaseManager.connect()

    val koin = startKoin<MyApp>().koin
    applyTransactionalProxies(koin)

    val port =
        System.getenv("PORT")?.toIntOrNull()
            ?: 8080

    embeddedServer(CIO, port = port) {
        configureDefaultModules()

        routing {
            configureSwaggerRoute("Shikkanime API", "1.0.0")

            ControllerBinder.register(
                this@routing,
                koin.getAll<IController>()
            )
        }
    }
        .start(wait = true)
}
