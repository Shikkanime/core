package fr.shikkanime.modules

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.controllers.site.SiteController
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.routes.Response
import freemarker.cache.ClassTemplateLoader
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.util.logging.Level

private val logger = LoggerFactory.getLogger("HTTP")

fun Application.configureHTTP() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        HttpMethod.DefaultMethods.forEach { allowMethod(it) }
        allowCredentials = true
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.log(Level.SEVERE, "Internal server error", cause)
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            val path = call.request.path()
            if (!isSitePath(path)) return@status
            val siteController = Constant.injector.getInstance(SiteController::class.java)
            logCallDetails(call, HttpStatusCode.NotFound)

            handleTemplateResponse(
                call,
                siteController,
                replacePathWithParameters(path, call.parameters.toMap()),
                Response.template(
                    HttpStatusCode.NotFound,
                    "/site/errors/404.ftl",
                    "Page introuvable"
                )
            )
        }
    }
    install(ContentNegotiation) {
        gson()
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        whitespaceStripping = true
    }
    install(CachingHeaders) {
    }
}

fun isSitePath(path: String): Boolean {
    return !path.startsWith("/api") && !path.startsWith(ADMIN) && !path.startsWith("/assets") && !path.startsWith("/feed") && !path.startsWith("/sitemap.xml")
}
