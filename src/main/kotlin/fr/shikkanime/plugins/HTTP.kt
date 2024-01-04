package fr.shikkanime.plugins

import freemarker.cache.ClassTemplateLoader
import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
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
        HttpMethod.DefaultMethods.forEach { allowMethod(it) }
        allowCredentials = true
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(ContentNegotiation) {
        gson {
        }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "api/swagger"
            forwardRoot = false
        }
        info {
            title = "Shikkanime API"
            version = "1.0"
            description = "API for testing and demonstration purposes"
        }
    }
}
