package fr.shikkanime.modules

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
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
import io.github.smiley4.ktorswaggerui.dsl.routing.*
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
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
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

private val logger = LoggerFactory.getLogger("Routing")
private val callStartTime = AttributeKey<ZonedDateTime>("CallStartTime")

fun Application.configureRouting() {
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)

    monitor.subscribe(RoutingRoot.RoutingCallStarted) { call ->
        call.attributes.put(callStartTime, ZonedDateTime.now())
        // If call is completed, the headers are already set
        if (call.response.status()?.value != null || !configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_SECURITY_HEADERS)) return@subscribe
        setSecurityHeaders(call)
    }

    monitor.subscribe(RoutingRoot.RoutingCallFinished) { call ->
        logCallDetails(call)
    }

    routing {
        staticResources("/assets", "assets") {
            preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = Constant.DEFAULT_CACHE_DURATION)) }
        }

        route("/api/openapi.json") {
            openApiSpec()
        }

        route("/api/swagger") {
            swaggerUI("/api/openapi.json")
        }

        createRoutes()
    }
}

private fun Application.setSecurityHeaders(call: ApplicationCall) {
    call.response.header(
        HttpHeaders.StrictTransportSecurity,
        "max-age=${Constant.DEFAULT_CACHE_DURATION}; includeSubDomains; preload"
    )

    call.response.header(
        "Content-Security-Policy",
        "default-src 'self';" +
                "style-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net;" +
                "font-src 'self' https://cdn.jsdelivr.net; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net;" +
                "img-src data: 'self' 'unsafe-inline' 'unsafe-eval' ${Constant.apiUrl} ${Constant.baseUrl};" +
                "connect-src 'self' ${Constant.apiUrl};"
    )

    call.response.header("X-Frame-Options", "DENY")
    call.response.header("X-Content-Type-Options", "nosniff")
    call.response.header("Referrer-Policy", "no-referrer")
    call.response.header("Permissions-Policy", "geolocation=(), microphone=()")
    call.response.header("X-XSS-Protection", "1; mode=block")
}

private fun logCallDetails(call: ApplicationCall) {
    val startTime = call.attributes[callStartTime]
    val httpMethod = call.request.httpMethod.value
    val status = call.response.status()?.value ?: 0
    val duration = ZonedDateTime.now().toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()
    val path = call.request.path()
    val ipAddress = call.request.header("X-Forwarded-For") ?: call.request.origin.remoteHost
    val userAgent = call.request.userAgent() ?: "Unknown"

    logger.info("[$ipAddress - $userAgent] ($status - $duration ms) $httpMethod ${call.request.origin.uri} -> $path")
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
                method.hasAnnotation<JWTAuthenticated>() -> {
                    val optional = method.findAnnotation<JWTAuthenticated>()!!.optional

                    authenticate(
                        "auth-jwt",
                        optional = optional,
                        build = routeHandler
                    )
                }
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
    val routeHandler: suspend RoutingContext.() -> Unit =
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
        val response = callMethodWithParameters(method, controller, call)

        if (method.hasAnnotation<Cached>()) {
            val cached = method.findAnnotation<Cached>()!!.maxAgeSeconds
            call.caching = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cached))
        }

        response.session?.let { call.sessions.set(it) }

        when (response.type) {
            ResponseType.MULTIPART -> handleMultipartResponse(call, response)
            ResponseType.TEMPLATE -> handleTemplateResponse(call, controller, replacedPath, response)
            ResponseType.REDIRECT -> call.respondRedirect(response.data as String, !replacedPath.startsWith("/admin"))
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

suspend fun handleTemplateResponse(
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

private suspend fun callMethodWithParameters(method: KFunction<*>, controller: Any, call: ApplicationCall): Response {
    val methodParams = method.parameters.associateWith { kParameter ->
        when {
            kParameter.name.isNullOrBlank() -> controller
            kParameter.hasAnnotation<JWTUser>() -> call.principal<JWTPrincipal>()?.payload?.getClaim("uuid")?.asString()?.let { UUID.fromString(it) }
            kParameter.hasAnnotation<AdminSessionUser>() -> call.principal<TokenDto>()
            kParameter.hasAnnotation<PathParam>() -> handlePathParam(kParameter, call)
            kParameter.hasAnnotation<QueryParam>() -> handleQueryParam(kParameter, call)
            kParameter.hasAnnotation<BodyParam>() -> handleBodyParam(kParameter, call)
            else -> throw Exception("Unknown parameter ${kParameter.name}")
        }
    }

    method.isAccessible = true

    try {
        return method.callBy(methodParams) as Response
    } catch (e: Exception) {
        logger.log(Level.SEVERE, "Error while calling method $method", e)
        return Response.internalServerError()
    }
}

private fun fromString(value: String?, type: KType): Any? {
    if (type.jvmErasure == Array<LangType>::class) {
        return value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map(LangType::valueOf)
            ?.toTypedArray()
    }

    val converters: Map<KClass<*>, (String?) -> Any?> = mapOf(
        UUID::class to { it?.let(UUID::fromString) },
        CountryCode::class to { CountryCode.fromNullable(it) },
        Platform::class to { Platform.fromNullable(it) },
        Status::class to { Status.fromNullable(it) },
        String::class to { it },
        Int::class to { it?.toIntOrNull() },
    )

    return converters.entries.firstOrNull { (kClass, _) ->
        kClass.starProjectedType.withNullability(true) == type || kClass.starProjectedType == type
    }?.value?.invoke(value)
}

private fun handlePathParam(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<PathParam>()?.name ?: kParameter.name
    val value = name?.let { call.parameters[name] }
    return fromString(value, kParameter.type)
}

private fun handleQueryParam(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<QueryParam>()?.name ?: kParameter.name
    val value = name?.let { call.request.queryParameters[it] }
    return fromString(value, kParameter.type)
}

private suspend fun handleBodyParam(kParameter: KParameter, call: ApplicationCall): Any {
    val type = kParameter.type

    return if (type.isSubtypeOf(MultiPartData::class.starProjectedType))
        call.receiveMultipart()
    else
        call.receive(type.jvmErasure)
}