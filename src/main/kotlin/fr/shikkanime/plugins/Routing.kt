package fr.shikkanime.plugins

import fr.shikkanime.controllers.AbstractController
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.Link
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.*
import fr.ziedelth.utils.routes.method.Delete
import fr.ziedelth.utils.routes.method.Get
import fr.ziedelth.utils.routes.method.Post
import fr.ziedelth.utils.routes.method.Put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

fun Application.configureRouting() {
    routing {
        staticResources("/assets", "assets")
        createRoutes()
    }
}

private fun Routing.createRoutes() {
    Constant.reflections.getSubTypesOf(AbstractController::class.java).forEach { controllerClass ->
        val controller = Constant.guice.getInstance(controllerClass)
        createControllerRoutes(controller)
    }
}

fun Routing.createControllerRoutes(controller: AbstractController) {
    val prefix = if (controller::class.hasAnnotation<Path>()) controller::class.findAnnotation<Path>()!!.value else ""
    val kMethods = controller::class.declaredFunctions.filter { it.hasAnnotation<Path>() }.toMutableSet()

    route(prefix) {
        kMethods.forEach { method ->
            val path = method.findAnnotation<Path>()!!.value

            if (method.hasAnnotation<Cached>()) {
                val cached = method.findAnnotation<Cached>()!!.maxAgeSeconds

                install(CachingHeaders) {
                    options { _, _ -> io.ktor.http.content.CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cached)) }
                }
            }

            if (method.hasAnnotation<Authenticated>()) {
                authenticate("auth-jwt") {
                    handleMethods(method, prefix, controller, path)
                }
            } else {
                handleMethods(method, prefix, controller, path)
            }
        }
    }
}

private fun Route.handleMethods(
    method: KFunction<*>,
    prefix: String,
    controller: AbstractController,
    path: String,
) {
    if (method.hasAnnotation<Get>()) {
        get(path) {
            handleRequest("GET", call, method, prefix, controller, path)
        }
    }

    if (method.hasAnnotation<Post>()) {
        post(path) {
            handleRequest("POST", call, method, prefix, controller, path)
        }
    }

    if (method.hasAnnotation<Put>()) {
        put(path) {
            handleRequest("PUT", call, method, prefix, controller, path)
        }
    }

    if (method.hasAnnotation<Delete>()) {
        delete(path) {
            handleRequest("DELETE", call, method, prefix, controller, path)
        }
    }
}

private suspend fun handleRequest(
    httpMethod: String,
    call: ApplicationCall,
    method: KFunction<*>,
    prefix: String,
    controller: AbstractController,
    path: String
) {
    val parameters = call.parameters.toMap()
    val replacedPath = replacePathWithParameters("$prefix$path", parameters)

    println("$httpMethod $replacedPath")

    try {
        val response = callMethodWithParameters(method, controller, call, parameters)

        when (response) {
            is MultipartResponse -> {
                call.respondBytes(response.image, response.contentType)
            }

            is TemplateResponse -> {
                val map = response.model.toMutableMap()

                map["links"] = Link.list().map { link ->
                    link.active = replacedPath.startsWith(link.href)
                    link
                }

                map["title"] = (if (!response.title.isNullOrBlank()) "${response.title} - " else "") + "Shikkanime"

                call.respond(FreeMarkerContent(response.template, map, ""))
            }

            is RedirectResponse -> {
                call.respondRedirect(response.path)
            }

            else -> {
                call.respond(response.status, response.data ?: "")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        call.respond(HttpStatusCode.BadRequest)
    }
}

private fun replacePathWithParameters(path: String, parameters: Map<String, List<String>>): String =
    parameters.keys.fold(path) { acc, param ->
        acc.replace("{$param}", parameters[param]!!.joinToString(", "))
    }

private suspend fun callMethodWithParameters(
    method: KFunction<*>,
    controller: AbstractController,
    call: ApplicationCall,
    parameters: Map<String, List<String>>
): Response {
    val methodParams = method.parameters.associateWith { kParameter ->
        when {
            kParameter.name.isNullOrBlank() -> {
                controller
            }

            method.hasAnnotation<Authenticated>() && kParameter.hasAnnotation<JWTUser>() -> {
                val jwtPrincipal = call.principal<JWTPrincipal>()
                UUID.fromString(jwtPrincipal!!.payload.getClaim("uuid").asString())
            }

            kParameter.hasAnnotation<BodyParam>() -> {
                when (kParameter.type.javaType) {
                    Array<UUID>::class.java -> call.receive<Array<UUID>>()
                    PlatformDto::class.java -> call.receive<PlatformDto>()
                    else -> call.receive<String>()
                }
            }

            kParameter.hasAnnotation<QueryParam>() -> {
                call.request.queryParameters[kParameter.name!!]
            }

            else -> {
                val value = parameters[kParameter.name]!!.first()

                val parsedValue: Any = when (kParameter.type.javaType) {
                    UUID::class.java -> UUID.fromString(value)
                    Int::class.java -> value.toInt()
                    else -> value
                }

                parsedValue
            }
        }
    }

    method.isAccessible = true
    return method.callBy(methodParams) as Response
}