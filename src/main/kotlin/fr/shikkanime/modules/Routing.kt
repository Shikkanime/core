package fr.shikkanime.modules

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.caches.BotDetectorCache
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.HttpHeader
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
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
private val attributeKey = AttributeKey<Boolean>("isBot")

fun Application.configureRouting() {
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)

    monitor.subscribe(RoutingRoot.RoutingCallStarted) { call ->
        call.attributes.put(callStartTime, ZonedDateTime.now())
        // If call is completed, the headers are already set
        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_SECURITY_HEADERS)) return@subscribe
        setSecurityHeaders(call, configCacheService)
    }

    monitor.subscribe(RoutingRoot.RoutingCallFinished) { call ->
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
    val authorizedDomains = configCacheService.getValueAsStringList(ConfigPropertyKey.AUTHORIZED_DOMAINS)
    val authorizedDomainsString = authorizedDomains.joinToString(StringUtils.SPACE_STRING).trim()

    call.response.header(
        HttpHeaders.StrictTransportSecurity,
        "max-age=${Constant.DEFAULT_CACHE_DURATION}; includeSubDomains; preload"
    )

    call.response.header(
        "Content-Security-Policy",
        "default-src 'self';" +
                "style-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net $authorizedDomainsString;" +
                "font-src 'self' https://cdn.jsdelivr.net $authorizedDomainsString; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net $authorizedDomainsString;" +
                "img-src data: 'self' 'unsafe-inline' 'unsafe-eval' ${Constant.apiUrl} ${Constant.baseUrl} $authorizedDomainsString;" +
                "connect-src 'self' ${Constant.apiUrl} $authorizedDomainsString;"
    )

    call.response.header("X-Frame-Options", "DENY")
    call.response.header("X-Content-Type-Options", "nosniff")
    call.response.header("Referrer-Policy", "no-referrer")
    call.response.header("Permissions-Policy", "geolocation=(), microphone=()")
    call.response.header("X-XSS-Protection", "1; mode=block")
}

fun logCallDetails(call: ApplicationCall, statusCode: HttpStatusCode? = null) {
    val startTime = call.attributes.getOrNull(callStartTime)
    val duration = startTime?.let { ZonedDateTime.now().toInstant().toEpochMilli() - it.toInstant().toEpochMilli() } ?: -1
    val ipAddress = call.request.header("X-Forwarded-For") ?: call.request.origin.remoteHost
    val userAgent = call.request.userAgent() ?: "Unknown"
    val isBot = call.attributes.getOrNull(attributeKey) == true
    val status = statusCode?.value ?: call.response.status()?.value ?: 0
    val httpMethod = call.request.httpMethod.value
    val uri = call.request.uri

    logger.info("[$ipAddress - $userAgent${if (isBot) " (BOT)" else StringUtils.EMPTY_STRING}] ($status - $duration ms) $httpMethod $uri")
}

private fun Routing.createRoutes() {
    Constant.reflections.getTypesAnnotatedWith(Controller::class.java).forEach { controllerClass ->
        val controller = Constant.injector.getInstance(controllerClass)
        createControllerRoutes(controller)
    }
}

fun Routing.createControllerRoutes(controller: Any) {
    val prefix = controller::class.findAnnotation<Controller>()?.value ?: StringUtils.EMPTY_STRING
    val kMethods = controller::class.declaredFunctions.filter { it.hasAnnotation<Path>() }

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
    val routeHandler: suspend RoutingContext.() -> Unit =
        { handleRequest(call, method, prefix, controller, path) }

    when {
        method.hasAnnotation<Get>() -> get(path, routeHandler)
        method.hasAnnotation<Post>() -> post(path, routeHandler)
        method.hasAnnotation<Put>() -> put(path, routeHandler)
        method.hasAnnotation<Delete>() -> delete(path, routeHandler)
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
            ResponseType.REDIRECT -> call.respondRedirect(response.data as String, !replacedPath.startsWith(ADMIN))
            else -> call.respond(response.status, response.data ?: StringUtils.EMPTY_STRING)
        }
    } catch (e: Exception) {
        logger.log(Level.SEVERE, "Error while calling method $method", e)
        call.respond(HttpStatusCode.InternalServerError)
    }
}

private suspend fun handleMultipartResponse(call: ApplicationCall, response: Response) {
    require(response.data is Map<*, *>) { "Data must be a map" }
    val map = response.data.toMap()
    call.respondBytes(map["image"] as ByteArray, map["contentType"] as ContentType)
}

suspend fun handleTemplateResponse(
    call: ApplicationCall,
    controller: Any,
    replacedPath: String,
    response: Response
) {
    val ipAddress = call.request.header("X-Forwarded-For") ?: call.request.origin.remoteHost
    val userAgent = call.request.userAgent() ?: "Unknown"
    require(response.data is Map<*, *>) { "Data must be a map" }
    val model = response.data["model"]
    require(model is Map<*, *>) { "Model must be a map" }
    val mutableMap = model.toMutableMap()

    var isBot = false
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
    val botDetectorCache = Constant.injector.getInstance(BotDetectorCache::class.java)

    if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.DISABLE_BOT_DETECTION) && botDetectorCache.isBot(clientIp = ipAddress, userAgent = userAgent)) {
        isBot = true
        call.attributes.put(attributeKey, true)
    }

    setGlobalAttributes(isBot, mutableMap, controller, replacedPath, response.data["title"] as String?)
    call.respond(response.status, FreeMarkerContent(response.data["template"] as String, mutableMap, StringUtils.EMPTY_STRING, response.contentType))
}

fun replacePathWithParameters(path: String, parameters: Map<String, List<String>>) =
    parameters.entries.fold(path) { acc, (param, values) ->
        acc.replace("{$param}", values.joinToString(", "))
    }

private suspend fun callMethodWithParameters(method: KFunction<*>, controller: Any, call: ApplicationCall): Response {
    val methodParams = method.parameters.associateWith { kParameter ->
        when {
            kParameter.name.isNullOrBlank() -> controller
            kParameter.hasAnnotation<JWTUser>() -> call.principal<JWTPrincipal>()?.payload?.getClaim("uuid")?.asString()?.let { UUID.fromString(it) }
            kParameter.hasAnnotation<AdminSessionUser>() -> call.principal<TokenDto>()
            kParameter.hasAnnotation<HttpHeader>() -> handleHttpHeader(kParameter, call)
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

    val converters = mapOf<KClass<*>, (String?) -> Any?>(
        UUID::class to { it?.let(UUID::fromString) },
        CountryCode::class to { CountryCode.fromNullable(it) },
        Platform::class to { Platform.fromNullable(it) },
        String::class to { it },
        Int::class to { it?.toIntOrNull() },
        Long::class to { it?.toLongOrNull() }
    )

    return converters[type.jvmErasure]?.invoke(value)
}

private fun handleHttpHeader(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<HttpHeader>()!!.name
    val value = call.request.headers[name]
    return fromString(value, kParameter.type)
}

private fun handlePathParam(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<PathParam>()?.name?.takeIf { it.isNotBlank() } ?: kParameter.name
    val value = name?.let { call.parameters[name] }
    return fromString(value, kParameter.type)
}

private fun handleQueryParam(kParameter: KParameter, call: ApplicationCall): Any? {
    val annotation = kParameter.findAnnotation<QueryParam>()
    val isMapType = kParameter.type.jvmErasure == Map::class

    if (annotation?.name.isNullOrBlank() && isMapType) {
        return call.request.queryParameters.toMap().mapValues { it.value.first() }
    }

    val paramName = annotation?.name?.takeIf { it.isNotBlank() } ?: kParameter.name
    val paramValue = paramName?.let { call.request.queryParameters[it] }
        ?: annotation?.defaultValue?.takeIf { it.isNotBlank() }

    return fromString(paramValue, kParameter.type)
}

private suspend fun handleBodyParam(kParameter: KParameter, call: ApplicationCall): Any {
    val type = kParameter.type

    return if (type.isSubtypeOf(MultiPartData::class.starProjectedType))
        call.receiveMultipart()
    else
        call.receive(type.jvmErasure)
}