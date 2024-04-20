package fr.shikkanime.modules

import fr.shikkanime.utils.Constant
import freemarker.cache.ClassTemplateLoader
import io.github.smiley4.ktorswaggerui.SwaggerUI
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
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            val path = call.request.path()

            if (path.contains(".")) {
                return@status
            }

            if (path.startsWith("/404")) {
                return@status
            }

            if (!path.startsWith("/api") && !path.startsWith("/admin")) {
                call.respondRedirect("/404")
            }
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
