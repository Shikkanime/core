package fr.shikkanime.plugins

import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.LinkObject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.github.smiley4.ktorswaggerui.dsl.*
import io.github.smiley4.ktorswaggerui.dsl.get
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import java.util.*
import java.util.logging.Level
import kotlin.collections.set
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

private val logger = LoggerFactory.getLogger("Routing")

fun Application.configureRouting() {
    routing {
        staticResources("/assets", "assets") {
            preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)

            cacheControl {
                listOf(CacheControl.MaxAge(maxAgeSeconds = 31536000))
            }
        }

        createRoutes()
    }
}

private fun Routing.createRoutes() {
    Constant.reflections.getTypesAnnotatedWith(Controller::class.java).forEach { controllerClass ->
        val controller = Constant.injector.getInstance(controllerClass)
        createControllerRoutes(controller)
    }
}

fun Routing.createControllerRoutes(controller: Any) {
    val prefix =
        if (controller::class.hasAnnotation<Controller>()) controller::class.findAnnotation<Controller>()!!.value else ""
    val kMethods = controller::class.declaredFunctions.filter { it.hasAnnotation<Path>() }.toMutableSet()

    if (prefix != "/") {
        route("$prefix/", {
            hidden = true
        }) {
            get {
                call.respondRedirect(prefix)
            }
        }
    }

    route(prefix) {
        kMethods.forEach { method ->
            val path = method.findAnnotation<Path>()!!.value

            if (method.hasAnnotation<JWTAuthenticated>()) {
                authenticate("auth-jwt") {
                    handleMethods(method, prefix, controller, path)
                }
            } else if (method.hasAnnotation<AdminSessionAuthenticated>()) {
                authenticate("auth-admin-session") {
                    handleMethods(method, prefix, controller, path)
                }
            } else {
                handleMethods(method, prefix, controller, path)
            }
        }
    }
}

private fun swagger(
    method: KFunction<*>,
    routeTags: List<String>,
    hiddenRoute: Boolean
): OpenApiRoute.() -> Unit {
    val openApi = method.findAnnotation<OpenAPI>() ?: return {
        tags = routeTags
        hidden = hiddenRoute
    }

    return {
        tags = routeTags
        hidden = hiddenRoute
        description = openApi.description
        request {
            method.parameters.filter { it.hasAnnotation<QueryParam>() }.forEach { parameter ->
                val qp = parameter.findAnnotation<QueryParam>()!!
                val name = qp.name.ifBlank { parameter.name!! }
                val type = if (qp.type == Unit::class) parameter.type.jvmErasure else qp.type

                queryParameter(name, type) {
                    description = qp.description
                    required = qp.required
                }
            }

            method.parameters.filter { it.hasAnnotation<PathParam>() }.forEach { parameter ->
                val pp = parameter.findAnnotation<PathParam>()!!
                val name = pp.name.ifBlank { parameter.name!! }
                val type = if (pp.type == Unit::class) parameter.type.jvmErasure else pp.type

                pathParameter(name, type) {
                    description = pp.description
                    required = true
                }
            }
        }
        response {
            openApi.responses.forEach { response ->
                HttpStatusCode.fromValue(response.status) to {
                    description = response.description

                    if (response.type.java.isArray) {
                        body(BodyTypeDescriptor.multipleOf(response.type.java.componentType.kotlin)) {
                            mediaType(
                                ContentType(
                                    response.contentType.split("/")[0],
                                    response.contentType.split("/")[1]
                                )
                            )
                        }
                    } else {
                        body(response.type) {
                            mediaType(
                                ContentType(
                                    response.contentType.split("/")[0],
                                    response.contentType.split("/")[1]
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Route.handleMethods(
    method: KFunction<*>,
    prefix: String,
    controller: Any,
    path: String,
) {
    val routeTags = listOf(controller.javaClass.simpleName.replace("Controller", ""))
    val hiddenRoute = !"$prefix$path".startsWith("/api")
    val swaggerBuilder = swagger(method, routeTags, hiddenRoute)

    if (method.hasAnnotation<Get>()) {
        get(path, swaggerBuilder) {
            handleRequest("GET", call, method, prefix, controller, path)
        }
    }

    if (method.hasAnnotation<Post>()) {
        post(path, swaggerBuilder) {
            handleRequest("POST", call, method, prefix, controller, path)
        }
    }

    if (method.hasAnnotation<Put>()) {
        put(path, swaggerBuilder) {
            handleRequest("PUT", call, method, prefix, controller, path)
        }
    }

    if (method.hasAnnotation<Delete>()) {
        delete(path, swaggerBuilder) {
            handleRequest("DELETE", call, method, prefix, controller, path)
        }
    }
}

private suspend fun handleRequest(
    httpMethod: String,
    call: ApplicationCall,
    method: KFunction<*>,
    prefix: String,
    controller: Any,
    path: String
) {
    val userAgent = call.request.userAgent()
    val parameters = call.parameters.toMap()
    val replacedPath = replacePathWithParameters("$prefix$path", parameters)

    logger.info("$httpMethod ${call.request.origin.uri} -> $replacedPath${if (userAgent != null) " ($userAgent)" else ""}")

    try {
        val response = callMethodWithParameters(method, controller, call, parameters)

        if (method.hasAnnotation<Cached>()) {
            val cached = method.findAnnotation<Cached>()!!.maxAgeSeconds
            call.caching = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cached))
        }

        response.session?.let { call.sessions.set(it) }

        when (response.type) {
            ResponseType.MULTIPART -> handleMultipartResponse(call, response)
            ResponseType.TEMPLATE -> handleTemplateResponse(call, controller, replacedPath, response)
            ResponseType.REDIRECT -> call.respondRedirect(response.data as String)
            else -> call.respond(response.status, response.data ?: "")
        }
    } catch (e: Exception) {
        logger.log(Level.SEVERE, "Error while calling method $method", e)
        call.respond(HttpStatusCode.BadRequest)
    }
}

private suspend fun handleMultipartResponse(call: ApplicationCall, response: Response) {
    val map = response.data as Map<String, Any>
    call.respondBytes(map["image"] as ByteArray, map["contentType"] as ContentType)
}

private suspend fun handleTemplateResponse(call: ApplicationCall, controller: Any, replacedPath: String, response: Response) {
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
    val map = response.data as Map<String, Any>
    val modelMap = (map["model"] as Map<String, Any>).toMutableMap()

    val list = if (controller.javaClass.simpleName.startsWith("Admin"))
        LinkObject.list().filter { it.href.startsWith("/admin") }
    else
        LinkObject.list().filter { !it.href.startsWith("/admin") }

    modelMap["links"] = list.map { link ->
        link.active = if (link.href == "/") replacedPath == link.href else replacedPath.startsWith(link.href)
        link
    }

    val title = map["title"] as String?
    modelMap["title"] = if (title?.contains(Constant.NAME) == true) title else (if (!title.isNullOrBlank()) "$title - " else "") + Constant.NAME
    modelMap["description"] = configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION) ?: ""
    configCacheService.getValueAsString(ConfigPropertyKey.GOOGLE_SITE_VERIFICATION_ID)?.let { modelMap["googleSiteVerification"] = it }

    call.respond(FreeMarkerContent(map["template"] as String, modelMap, "", response.contentType))
}

private fun replacePathWithParameters(path: String, parameters: Map<String, List<String>>): String =
    parameters.keys.fold(path) { acc, param ->
        acc.replace("{$param}", parameters[param]!!.joinToString(", "))
    }

private suspend fun callMethodWithParameters(
    method: KFunction<*>,
    controller: Any,
    call: ApplicationCall,
    parameters: Map<String, List<String>>
): Response {
    val methodParams = method.parameters.associateWith { kParameter ->
        when {
            kParameter.name.isNullOrBlank() -> controller
            method.hasAnnotation<JWTAuthenticated>() && kParameter.hasAnnotation<JWTUser>() ->
                UUID.fromString(call.principal<JWTPrincipal>()!!.payload.getClaim("uuid").asString())

            method.hasAnnotation<AdminSessionAuthenticated>() && kParameter.hasAnnotation<AdminSessionUser>() ->
                call.principal<TokenDto>()

            kParameter.hasAnnotation<BodyParam>() -> handleBodyParam(kParameter, call)
            kParameter.hasAnnotation<QueryParam>() -> handleQueryParam(kParameter, call)
            kParameter.hasAnnotation<PathParam>() -> handlePathParam(kParameter, parameters)
            else -> throw Exception("Unknown parameter ${kParameter.name}")
        }
    }

    method.isAccessible = true
    return method.callBy(methodParams) as Response
}

private suspend fun handleBodyParam(kParameter: KParameter, call: ApplicationCall): Any {
    return when (kParameter.type.javaType) {
        Array<UUID>::class.java -> call.receive<Array<UUID>>()
        Parameters::class.java -> call.receiveParameters()
        else -> call.receive<String>()
    }
}

private fun handleQueryParam(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<QueryParam>()!!.name.ifBlank { kParameter.name!! }
    val queryParamValue = call.request.queryParameters[name]

    return when (kParameter.type) {
        Int::class.starProjectedType.withNullability(true) -> queryParamValue?.toIntOrNull()
        String::class.starProjectedType.withNullability(true) -> queryParamValue
        CountryCode::class.starProjectedType.withNullability(true) -> CountryCode.fromNullable(queryParamValue)
        UUID::class.starProjectedType.withNullability(true) -> if (queryParamValue.isNullOrBlank()) null else UUID.fromString(queryParamValue)
        else -> throw Exception("Unknown type ${kParameter.type}")
    }
}

private fun handlePathParam(kParameter: KParameter, parameters: Map<String, List<String>>): Any? {
    val name = kParameter.findAnnotation<PathParam>()!!.name.ifBlank { kParameter.name!! }
    val pathParamValue = parameters[name]?.firstOrNull()

    return when (kParameter.type.javaType) {
        UUID::class.java -> UUID.fromString(pathParamValue)
        Platform::class.java -> Platform.valueOf(pathParamValue!!)
        String::class.java -> pathParamValue
        else -> throw Exception("Unknown type ${kParameter.type}")
    }
}