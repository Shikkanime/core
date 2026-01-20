package fr.shikkanime.modules

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.mappings.EpisodeAggregationResultDto
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.caches.BotDetectorCache
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
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
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

private val logger = LoggerFactory.getLogger("Routing")
private val callStartTime = AttributeKey<ZonedDateTime>("CallStartTime")
private val attributeKey = AttributeKey<Boolean>("isBot")
private val fromStringConverters = mapOf<KClass<*>, (String?) -> Any?>(
    UUID::class to { it?.let(UUID::fromString) },
    CountryCode::class to { CountryCode.fromNullable(it) },
    Platform::class to { Platform.fromNullable(it) },
    String::class to { it },
    Int::class to { it?.toIntOrNull() },
    Long::class to { it?.toLongOrNull() }
)
private val jvmErasureCache = ConcurrentHashMap<KParameter, KClass<*>>()
private val mapKClass = Map::class
private val arrayLangTypeKClass = Array<LangType>::class
private val arrayEpisodeAggreationResultDtoKClass = Array<EpisodeAggregationResultDto>::class
private val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
private val database = Constant.injector.getInstance(Database::class.java)
private val botDetectorCache = Constant.injector.getInstance(BotDetectorCache::class.java)

private const val STRICT_TRANSPORT_SECURITY_VALUE = "max-age=${Constant.DEFAULT_CACHE_DURATION}; includeSubDomains; preload"
private const val CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy"
private const val X_FRAME_OPTIONS_HEADER = "X-Frame-Options"
private const val X_FRAME_OPTIONS_VALUE = "DENY"
private const val X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options"
private const val X_CONTENT_TYPE_OPTIONS_VALUE = "nosniff"
private const val REFERRER_POLICY_HEADER = "Referrer-Policy"
private const val REFERRER_POLICY_VALUE = "no-referrer"
private const val PERMISSIONS_POLICY_HEADER = "Permissions-Policy"
private const val PERMISSIONS_POLICY_VALUE = "geolocation=(), microphone=()"
private const val X_XSS_PROTECTION_HEADER = "X-XSS-Protection"
private const val X_XSS_PROTECTION_VALUE = "1; mode=block"

fun Application.configureRouting() {
    monitor.subscribe(RoutingRoot.RoutingCallStarted) { call ->
        call.attributes.put(callStartTime, ZonedDateTime.now())
        // If call is completed, the headers are already set
        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_SECURITY_HEADERS)) return@subscribe
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

        createRoutes()
    }
}

private fun setSecurityHeaders(call: ApplicationCall) {
    val authorizedDomains = configCacheService.getValueAsStringList(ConfigPropertyKey.AUTHORIZED_DOMAINS).joinToString(StringUtils.SPACE_STRING).trim()

    call.response.headers.apply {
        append(HttpHeaders.StrictTransportSecurity, STRICT_TRANSPORT_SECURITY_VALUE)
        append(CONTENT_SECURITY_POLICY_HEADER, "default-src 'self'; " +
                "style-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net $authorizedDomains; " +
                "font-src 'self' https://cdn.jsdelivr.net $authorizedDomains; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net $authorizedDomains; " +
                "img-src data: 'self' 'unsafe-inline' 'unsafe-eval' ${Constant.apiUrl} ${Constant.baseUrl} $authorizedDomains; " +
                "connect-src 'self' ${Constant.apiUrl} $authorizedDomains;")
        append(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE)
        append(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE)
        append(REFERRER_POLICY_HEADER, REFERRER_POLICY_VALUE)
        append(PERMISSIONS_POLICY_HEADER, PERMISSIONS_POLICY_VALUE)
        append(X_XSS_PROTECTION_HEADER, X_XSS_PROTECTION_VALUE)
    }
}

fun logCallDetails(call: ApplicationCall, statusCode: HttpStatusCode? = null) {
    val attributes = call.attributes
    val request = call.request

    val startTime = attributes.getOrNull(callStartTime)
    val duration = startTime?.let { ZonedDateTime.now().toInstant().toEpochMilli() - it.toInstant().toEpochMilli() } ?: -1
    val ipAddress = request.header(HttpHeaders.XForwardedFor) ?: request.origin.remoteHost
    val userAgent = request.userAgent() ?: "Unknown"
    val isBot = attributes.getOrNull(attributeKey) == true
    val status = statusCode?.value ?: call.response.status()?.value ?: 0
    val httpMethod = request.httpMethod.value
    val uri = request.uri

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
                method.hasAnnotation<AdminSessionAuthenticated>() || controller::class.hasAnnotation<AdminSessionAuthenticated>() -> authenticate(
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

    database.withAsyncContext {
        try {
            val response = callMethodWithParameters(method, controller, call, parameters)

            if (method.hasAnnotation<Cached>()) {
                val cached = method.findAnnotation<Cached>()!!.maxAgeSeconds
                call.caching = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cached))
            }

            response.session?.let(call.sessions::set)

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
    val ipAddress = call.request.header(HttpHeaders.XForwardedFor) ?: call.request.origin.remoteHost
    val userAgent = call.request.userAgent() ?: "Unknown"
    require(response.data is Map<*, *>) { "Data must be a map" }
    val model = response.data["model"]
    require(model is Map<*, *>) { "Model must be a map" }
    val mutableMap = model.toMutableMap()

    var isBot = false

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

private suspend fun callMethodWithParameters(method: KFunction<*>, controller: Any, call: ApplicationCall, parameters: Map<String, List<String>>): Response {
    val methodParams = method.parameters.associateWith { kParameter ->
        when {
            kParameter.name.isNullOrBlank() -> controller
            kParameter.hasAnnotation<JWTUser>() -> call.principal<JWTPrincipal>()?.payload?.getClaim("uuid")?.asString()?.let(UUID::fromString)
            kParameter.hasAnnotation<AdminSessionUser>() -> call.principal<TokenDto>()
            kParameter.hasAnnotation<HttpHeader>() -> handleHttpHeader(kParameter, call)
            kParameter.hasAnnotation<PathParam>() -> handlePathParam(kParameter, parameters)
            kParameter.hasAnnotation<QueryParam>() -> handleQueryParam(kParameter, parameters)
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

private fun fromString(value: String?, jvmErasure: KClass<*>): Any? {
    if (jvmErasure == arrayLangTypeKClass) {
        return value?.takeIf { it.isNotBlank() }
            ?.split(StringUtils.COMMA_STRING)
            ?.map(LangType::valueOf)
            ?.toTypedArray()
    }

    return fromStringConverters[jvmErasure]?.invoke(value)
}

private fun handleHttpHeader(kParameter: KParameter, call: ApplicationCall): Any? {
    val name = kParameter.findAnnotation<HttpHeader>()!!.name
    val value = call.request.headers[name]
    return fromString(value, jvmErasureCache.getOrPut(kParameter) { kParameter.type.jvmErasure })
}

private fun handlePathParam(kParameter: KParameter, parameters: Map<String, List<String>>): Any? {
    val name = kParameter.findAnnotation<PathParam>()?.name?.takeIf { it.isNotBlank() } ?: kParameter.name
    val value = name?.let { parameters[name]?.firstOrNull() }
    return fromString(value, jvmErasureCache.getOrPut(kParameter) { kParameter.type.jvmErasure })
}

private fun handleQueryParam(kParameter: KParameter, parameters: Map<String, List<String>>): Any? {
    val annotation = kParameter.findAnnotation<QueryParam>()
    val jvmErasure = jvmErasureCache.getOrPut(kParameter) { kParameter.type.jvmErasure }
    val annotationName = annotation?.name

    if (annotationName.isNullOrBlank() && jvmErasure == mapKClass) {
        return parameters.mapValues { it.value.first() }
    }

    val paramName = annotationName?.takeIf { it.isNotBlank() } ?: kParameter.name
    val paramValue = paramName?.let { parameters[it]?.firstOrNull() } ?: annotation?.defaultValue?.takeIf { it.isNotBlank() }

    return fromString(paramValue, jvmErasure)
}

private suspend fun handleBodyParam(kParameter: KParameter, call: ApplicationCall): Any {
    val type = kParameter.type

    return if (type.isSubtypeOf(MultiPartData::class.starProjectedType))
        call.receiveMultipart()
    else {
        val jvmErasure = type.jvmErasure

        if (jvmErasure == arrayEpisodeAggreationResultDtoKClass) {
            call.receive<List<EpisodeAggregationResultDto>>()
                .toTypedArray()
        } else {
            call.receive(jvmErasure)
        }
    }
}