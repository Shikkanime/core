package fr.shikkanime.modules

import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.ConfigDto
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.dtos.enums.Status
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
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.github.smiley4.ktorswaggerui.dsl.*
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
import io.ktor.util.pipeline.*
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType

private val logger = LoggerFactory.getLogger("Routing")
private val callStartTime = AttributeKey<ZonedDateTime>("CallStartTime")

fun Application.configureRouting() {
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)

    environment.monitor.subscribe(Routing.RoutingCallStarted) { call ->
        call.attributes.put(callStartTime, ZonedDateTime.now())
        setSecurityHeaders(call, configCacheService)
    }

    environment.monitor.subscribe(Routing.RoutingCallFinished) { call ->
        logCallDetails(call)
    }

    routing {
        staticResources("/assets", "assets") {
            preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = Constant.DEFAULT_CACHE_DURATION)) }
        }

        createRoutes()
    }
}

private fun setSecurityHeaders(call: ApplicationCall, configCacheService: ConfigCacheService) {
    call.response.pipeline.intercept(ApplicationSendPipeline.Transform) {
        context.response.header(
            HttpHeaders.StrictTransportSecurity,
            "max-age=${Constant.DEFAULT_CACHE_DURATION}; includeSubDomains; preload"
        )

        context.response.header(
            "Content-Security-Policy",
            "default-src 'self';" +
                    "style-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net;" +
                    "font-src 'self' https://cdn.jsdelivr.net; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net;" +
                    "img-src data: 'self' 'unsafe-inline' 'unsafe-eval' ${Constant.apiUrl} ${Constant.baseUrl};" +
                    "connect-src 'self' ${Constant.apiUrl} ${configCacheService.getValueAsString(ConfigPropertyKey.ANALYTICS_API) ?: ""};"
        )

        context.response.header("X-Frame-Options", "DENY")
        context.response.header("X-Content-Type-Options", "nosniff")
        context.response.header("Referrer-Policy", "no-referrer")
        context.response.header("Permissions-Policy", "geolocation=(), microphone=()")
        context.response.header("X-XSS-Protection", "1; mode=block")
    }
}

private fun logCallDetails(call: ApplicationCall) {
    val startTime = call.attributes[callStartTime]
    val duration = ZonedDateTime.now().toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()
    val path = call.request.path()
    val httpMethod = call.request.httpMethod.value
    val userAgent = call.request.userAgent()
    val status = call.response.status()?.value ?: 0

    logger.info("$httpMethod ${call.request.origin.uri} [$status - $duration ms] -> $path${if (userAgent != null) " ($userAgent)" else ""}")
}

private fun Routing.createRoutes() {
    Constant.reflections.getTypesAnnotatedWith(Controller::class.java).forEach { controllerClass ->
        val controller = Constant.injector.getInstance(controllerClass)
        createControllerRoutes(controller)
    }
}

fun Routing.createControllerRoutes(controller: Any) {
    val prefix = controller::class.findAnnotation<Controller>()?.value ?: ""
    val kMethods = controller::class.declaredFunctions.filter { it.hasAnnotation<Path>() }.toMutableSet()

    route(prefix) {
        kMethods.forEach { method ->
            val path = method.findAnnotation<Path>()!!.value
            val routeHandler: Route.() -> Unit = { handleMethods(method, prefix, controller, path) }

            when {
                method.hasAnnotation<JWTAuthenticated>() -> authenticate("auth-jwt", build = routeHandler)
                method.hasAnnotation<AdminSessionAuthenticated>() -> authenticate(
                    "auth-admin-session",
                    build = routeHandler
                )

                else -> routeHandler()
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
    val routeHandler: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit =
        { handleRequest(call, method, prefix, controller, path) }

    when {
        method.hasAnnotation<Get>() -> get(path, swaggerBuilder, routeHandler)
        method.hasAnnotation<Post>() -> post(path, swaggerBuilder, routeHandler)
        method.hasAnnotation<Put>() -> put(path, swaggerBuilder, routeHandler)
        method.hasAnnotation<Delete>() -> delete(path, swaggerBuilder, routeHandler)
    }
}

private suspend fun handleRequest(
    call: ApplicationCall,
    method: KFunction<*>,
    prefix: String,
    controller: Any,
    path: String
) {
    val parameters = call.parameters.toMap()
    val replacedPath = replacePathWithParameters("$prefix$path", parameters)

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
        call.respond(HttpStatusCode.InternalServerError)
    }
}

private suspend fun handleMultipartResponse(call: ApplicationCall, response: Response) {
    val map = response.data as Map<String, Any> // NOSONAR
    call.respondBytes(map["image"] as ByteArray, map["contentType"] as ContentType)
}

private suspend fun handleTemplateResponse(
    call: ApplicationCall,
    controller: Any,
    replacedPath: String,
    response: Response
) {
    val map = response.data as Map<String, Any> // NOSONAR
    val modelMap = (map["model"] as Map<String, Any?>).toMutableMap() // NOSONAR
    setGlobalAttributes(modelMap, controller, replacedPath, map["title"] as String?)
    call.respond(response.status, FreeMarkerContent(map["template"] as String, modelMap, "", response.contentType))
}

private fun replacePathWithParameters(path: String, parameters: Map<String, List<String>>) =
    parameters.entries.fold(path) { acc, (param, values) ->
        acc.replace("{$param}", values.joinToString(", "))
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
            kParameter.hasAnnotation<JWTUser>() -> UUID.fromString(
                call.principal<JWTPrincipal>()!!.payload.getClaim("uuid").asString()
            )

            kParameter.hasAnnotation<AdminSessionUser>() -> call.principal<TokenDto>()
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
        ConfigDto::class.java -> call.receive<ConfigDto>()
        AnimeDto::class.java -> call.receive<AnimeDto>()
        EpisodeMappingDto::class.java -> call.receive<EpisodeMappingDto>()
        else -> call.receive<String>()
    }
}

private fun handleQueryParam(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<QueryParam>()?.name ?: kParameter.name
    val queryParamValue = name?.let { call.request.queryParameters[it] }

    return when (kParameter.type) {
        Int::class.starProjectedType.withNullability(true) -> queryParamValue?.toIntOrNull()
        String::class.starProjectedType.withNullability(true) -> queryParamValue
        CountryCode::class.starProjectedType.withNullability(true) -> CountryCode.fromNullable(queryParamValue)
        UUID::class.starProjectedType.withNullability(true) -> queryParamValue?.let { UUID.fromString(it) }
        Status::class.starProjectedType.withNullability(true) -> queryParamValue?.let {
            try {
                Status.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        else -> throw Exception("Unknown type ${kParameter.type}")
    }
}

private fun handlePathParam(kParameter: KParameter, parameters: Map<String, List<String>>): Any? {
    val name = kParameter.findAnnotation<PathParam>()?.name ?: kParameter.name
    val pathParamValue = parameters[name]?.firstOrNull()

    return when (kParameter.type.javaType) {
        UUID::class.java -> pathParamValue?.let { UUID.fromString(it) }
        Platform::class.java -> pathParamValue?.let { Platform.valueOf(it) }
        String::class.java -> pathParamValue
        else -> throw Exception("Unknown type ${kParameter.type}")
    }
}