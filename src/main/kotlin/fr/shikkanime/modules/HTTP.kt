package fr.shikkanime.modules

import fr.shikkanime.controllers.site.SiteController
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.routes.Response
import freemarker.cache.ClassTemplateLoader
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.data.AuthScheme
import io.github.smiley4.ktorswaggerui.data.AuthType
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
            if (isNotSiteRoute(path, "404")) return@status

            if (call.response.status() == HttpStatusCode.NotFound) {
                val siteController = Constant.injector.getInstance(SiteController::class.java)
                handleTemplateResponse(
                    call,
                    siteController,
                    "/404",
                    Response.template(HttpStatusCode.NotFound, "/site/errors/404.ftl", "Page introuvable")
                )
                return@status
            }

            call.respondRedirect("/404")
        }
    }
    install(ContentNegotiation) {
        gson {
        }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        whitespaceStripping = true
    }
    install(CachingHeaders) {
    }
    install(SwaggerUI) {
        securityScheme("BearerAuth") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
            bearerFormat = "jwt"
        }
        swagger {
            swaggerUrl = "api/swagger"
            forwardRoot = false
        }
        info {
            title = "${Constant.NAME} API"
            version = "1.0.0"
            description = "API for testing and demonstration purposes"
        }
        server {
            url = Constant.baseUrl
        }
    }
}

private fun isNotSiteRoute(path: String, errorCode: String): Boolean {
    return path.contains(".") || path.startsWith("/$errorCode") || path.startsWith("/api") || path.startsWith("/admin")
}
