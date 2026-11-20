package fr.shikkanime.admin

import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

internal const val WEB_APP_PACKAGE = "webapp"

/**
 * Directory served instead of the packaged interface when `WEB_APP_DIR` is set.
 *
 * It is the escape hatch for a run that does not go through the distribution - an IDE run, or a
 * compiled app moved by hand - where the classpath carries no `webapp` package.
 *
 * @return the directory `WEB_APP_DIR` points to, or `null` when the variable is unset or blank.
 * @see [WEB_APP_DIR_ENV]
 */
private fun servedDirectory(): File? =
    System.getenv(WEB_APP_DIR_ENV)?.takeIf(String::isNotBlank)?.let(::File)

internal const val WEB_APP_DIR_ENV = "WEB_APP_DIR"

/**
 * Fails when no compiled interface can be served.
 *
 * An unserved interface answers `404` on `/`, which reads like a routing bug while the real cause is
 * a missing build step: the message names both ways to provide it.
 *
 * @return nothing; the function throws when no interface can be served.
 * @see [servedDirectory]
 */
internal fun requireWebApp() {
    val directory = servedDirectory()

    if (directory != null) {
        require(directory.resolve("index.html").isFile) {
            "The administration interface is not packaged: $WEB_APP_DIR_ENV=$directory holds no index.html."
        }
        return
    }

    val packaged = object {}.javaClass.classLoader.getResource("$WEB_APP_PACKAGE/index.html")

    requireNotNull(packaged) {
        "The administration interface is not packaged: run ./gradlew :admin:installDist " +
            "(or :admin:run, which packages it), or point $WEB_APP_DIR_ENV at a directory holding index.html. " +
            "The interface reaches the classpath through :admin:processResources, not from build/webapp directly."
    }
}

/**
 * Mounts the administration single-page application.
 *
 * `index.html` answers the root and every path the Flutter build does not own, which is what a
 * single-page application expects from its host. The interface is served from the `webapp`
 * resource package the Gradle build fills with the compiled Flutter administration, so the server
 * and the interface it administers share one origin and one deployment; `WEB_APP_DIR` points at a
 * directory instead for runs outside the distribution.
 *
 * @receiver routing tree of the administration application.
 * @return nothing; the routes are mounted as a side effect.
 * @see [WEB_APP_PACKAGE]
 * @see [servedDirectory]
 */
internal fun Routing.webApp() {
    val directory = servedDirectory()

    if (directory != null) {
        staticFiles("/", directory) {
            default("index.html")
        }
        return
    }

    staticResources("/", WEB_APP_PACKAGE) {
        default("index.html")
    }
}
