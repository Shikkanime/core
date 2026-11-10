package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.ktor.createHttpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Smart HTTP client wrapper designed for scheduled jobs polling remote APIs.
 *
 * Provides:
 * - Content change detection via HTTP 304, ETags, Last-Modified, and body hash comparisons.
 * - Single-flight request coalescing to prevent concurrent duplicate network calls.
 * - Memory-bounded LRU cache with automatic eviction and stale expiration without third-party libraries.
 */
@Single
class SmartHttpClient(
    val client: HttpClient = createHttpClient(),
    val maxCacheCapacity: Int = 1000,
    val maxStaleRetention: Duration = 24.hours
) {
    data class HttpCacheEntry(
        val data: Any,
        val contentHash: Int,
        val etag: String?,
        val lastModified: String?,
        val expiresAtMillis: Long,
        val fetchedAtMillis: Long
    )

    /**
     * Thread-safe LRU memory cache built upon standard JDK [LinkedHashMap] with access-order eviction.
     * Automatically evicts the least recently used entries when [capacity] is reached and purges
     * entries exceeding [maxStaleMillis].
     */
    class LruMemoryCache(
        private val capacity: Int,
        private val maxStaleMillis: Long
    ) {
        private val map = object : LinkedHashMap<String, HttpCacheEntry>(capacity, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, HttpCacheEntry>?
            ): Boolean = size > capacity
        }

        /**
         * Returns the entry stored for the given key, or `null` when it is absent or stale.
         *
         * A hit refreshes the recency of the entry in the LRU order.
         *
         * @param key the cache key to look up.
         * @return the stored [HttpCacheEntry], or `null`.
         * @see [HttpCacheEntry]
         */
        @Synchronized
        operator fun get(key: String): HttpCacheEntry? {
            val entry = map[key] ?: return null
            val nowMillis = Clock.System.now().toEpochMilliseconds()
            if (nowMillis > entry.expiresAtMillis + maxStaleMillis) {
                map.remove(key)
                return null
            }
            return entry
        }

        /**
         * Stores the entry for the given key after purging the expired entries.
         *
         * @param key the cache key to store under.
         * @param value the [HttpCacheEntry] to store.
         * @see [HttpCacheEntry]
         */
        @Synchronized
        operator fun set(key: String, value: HttpCacheEntry) {
            cleanUpExpired(Clock.System.now().toEpochMilliseconds())
            map[key] = value
        }

        /**
         * Removes the entry stored for the given key.
         *
         * @param key the cache key to remove.
         * @return the removed entry, or null when the key is absent.
         * @see [HttpCacheEntry]
         */
        @Suppress("unused")
        @Synchronized
        fun remove(key: String): HttpCacheEntry? =
            map.remove(key)

        /**
         * Removes every entry from the cache.
         *
         * @return nothing.
         * @see [HttpCacheEntry]
         */
        @Suppress("unused")
        @Synchronized
        fun clear(): Unit =
            map.clear()

        /**
         * Removes every entry whose freshness window plus the stale retention has elapsed.
         *
         * @param nowMillis the reference timestamp in epoch milliseconds.
         * @return nothing.
         * @see [HttpCacheEntry]
         */
        @Synchronized
        fun cleanUpExpired(nowMillis: Long = Clock.System.now().toEpochMilliseconds()) {
            val iterator = map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next().value
                if (nowMillis > entry.expiresAtMillis + maxStaleMillis) {
                    iterator.remove()
                }
            }
        }

        val size: Int
            @Synchronized get() = map.size
    }

    val logger = LoggerFactory.getLogger()
    val cache = LruMemoryCache(maxCacheCapacity, maxStaleRetention.inWholeMilliseconds)
    val inFlight = ConcurrentHashMap<String, CompletableDeferred<SmartResponse<*>>>()

    /**
     * Performs a cached GET request with content change detection.
     *
     * @param T type of the expected response payload.
     * @param url the remote URL to query.
     * @param cacheKey the cache key identifying this payload.
     * @param ttl the freshness duration of the cached payload.
     * @param staleOnError whether a stale cached payload may be served on request failure.
     * @param headers the request headers sent with the query.
     * @return the [SmartResponse] carrying the payload and its change flag.
     * @see [SmartResponse]
     * @see [cachedRequest]
     */
    suspend inline fun <reified T : Any> get(
        url: String,
        cacheKey: String = url,
        ttl: Duration = Duration.ZERO,
        staleOnError: Boolean = true,
        headers: Map<String, String> = emptyMap()
    ): SmartResponse<T> =
        cachedRequest(url, HttpMethod.Get, cacheKey, ttl, staleOnError, headers)

    /**
     * Performs a cached POST request with content change detection.
     *
     * The body is sent as [contentType] encoded content: [String] bodies are wrapped in a
     * [TextContent] (covering the form urlencoded payloads of token endpoints), while any other
     * body is serialized by the client content negotiation, typically as JSON.
     *
     * @param T type of the expected response payload.
     * @param url the remote URL to query.
     * @param body the request body sent with the query.
     * @param cacheKey the cache key identifying this payload.
     * @param ttl the freshness duration of the cached payload.
     * @param staleOnError whether a stale cached payload may be served on request failure.
     * @param headers the request headers sent with the query.
     * @param contentType the content type of the request body.
     * @return the [SmartResponse] carrying the payload and its change flag.
     * @see [SmartResponse]
     * @see [cachedRequest]
     */
    suspend inline fun <reified T : Any> post(
        url: String,
        body: Any,
        cacheKey: String = url,
        ttl: Duration = Duration.ZERO,
        staleOnError: Boolean = true,
        headers: Map<String, String> = emptyMap(),
        contentType: ContentType = ContentType.Application.Json
    ): SmartResponse<T> =
        cachedRequest(url, HttpMethod.Post, cacheKey, ttl, staleOnError, headers) {
            if (body is String) {
                setBody(TextContent(body, contentType))
            } else {
                this.contentType(contentType)
                setBody(body)
            }
        }

    /**
     * Shared implementation of the cached requests of this client.
     *
     * Applies the memory cache, the single-flight coalescing, the conditional headers and the
     * stale-on-error fallback to every cached call, whether it is a GET or a POST.
     *
     * @param T type of the expected response payload.
     * @param url the remote URL to query.
     * @param httpMethod the HTTP method of the request.
     * @param cacheKey the cache key identifying this payload.
     * @param ttl the freshness duration of the cached payload.
     * @param staleOnError whether a stale cached payload may be served on request failure.
     * @param headers the request headers sent with the query.
     * @param configure additional configuration of the request builder.
     * @return the [SmartResponse] carrying the payload and its change flag.
     * @see [SmartResponse]
     */
    @PublishedApi
    internal suspend inline fun <reified T : Any> cachedRequest(
        url: String,
        httpMethod: HttpMethod,
        cacheKey: String,
        ttl: Duration,
        staleOnError: Boolean,
        headers: Map<String, String>,
        configure: HttpRequestBuilder.() -> Unit = {}
    ): SmartResponse<T> {
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        // Return immediately if cached data is still fresh within its TTL
        val cached = cache[cacheKey]
        if (cached != null && nowMillis < cached.expiresAtMillis) {
            logger.fine("Cache hit (fresh) for key: $cacheKey")
            return SmartResponse(
                cached.data as T,
                hasChanged = false,
                fetchedAtMillis = cached.fetchedAtMillis
            )
        }

        // Coalesce concurrent requests for the same key to avoid duplicate network calls
        while (true) {
            val deferred = CompletableDeferred<SmartResponse<*>>()
            val inProgress = inFlight.putIfAbsent(cacheKey, deferred)

            if (inProgress != null) {
                logger.fine("Coalescing concurrent request for key: $cacheKey")
                try {
                    @Suppress("UNCHECKED_CAST")
                    return inProgress.await() as SmartResponse<T>
                } catch (e: CancellationException) {
                    if (currentCoroutineContext().isActive) {
                        // Leader coroutine was cancelled; retry to take over execution or join the next attempt
                        continue
                    }
                    throw e
                }
            }

            // Leader coroutine performing the actual HTTP call
            try {
                logger.info("Executing ${httpMethod.value} request to $url (cacheKey: $cacheKey)")
                val response = client.request(url) {
                    method = httpMethod
                    headers.forEach { (key, value) -> header(key, value) }

                    if (cached != null) {
                        cached.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                        cached.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
                    }

                    configure()
                }

                val now = Clock.System.now().toEpochMilliseconds()

                if (cached != null && response.status == HttpStatusCode.NotModified) {
                    logger.fine("Cache revalidated (304 Not Modified) for key: $cacheKey")
                    val updated = cached.copy(expiresAtMillis = now + ttl.inWholeMilliseconds)
                    cache[cacheKey] = updated
                    val result = SmartResponse(
                        updated.data as T,
                        hasChanged = false,
                        fetchedAtMillis = updated.fetchedAtMillis
                    )
                    deferred.complete(result)
                    return result
                }

                if (response.status.isSuccess()) {
                    val etag = response.headers[HttpHeaders.ETag]
                    val lastModified = response.headers[HttpHeaders.LastModified]
                    val bodyText = response.bodyAsText()
                    val contentHash = bodyText.hashCode()

                    // Avoid deserialization and downstream processing if content hash matches previous response
                    if (cached != null && cached.contentHash == contentHash) {
                        logger.fine("Content unchanged (Hash match) for key: $cacheKey")
                        val updated = cached.copy(
                            etag = etag ?: cached.etag,
                            lastModified = lastModified ?: cached.lastModified,
                            expiresAtMillis = now + ttl.inWholeMilliseconds
                        )
                        cache[cacheKey] = updated
                        val result = SmartResponse(
                            updated.data as T,
                            hasChanged = false,
                            fetchedAtMillis = updated.fetchedAtMillis
                        )
                        deferred.complete(result)
                        return result
                    }

                    val parsedData: T = if (T::class == String::class) {
                        bodyText as T
                    } else {
                        response.body<T>()
                    }

                    cache[cacheKey] = HttpCacheEntry(
                        data = parsedData,
                        contentHash = contentHash,
                        etag = etag,
                        lastModified = lastModified,
                        expiresAtMillis = now + ttl.inWholeMilliseconds,
                        fetchedAtMillis = now
                    )

                    val result = SmartResponse(
                        parsedData,
                        hasChanged = true,
                        fetchedAtMillis = now
                    )
                    deferred.complete(result)
                    return result
                }

                if (cached != null && staleOnError) {
                    logger.warning(
                        "HTTP ${response.status.value} from $url, " +
                            "serving stale cache for key: $cacheKey"
                    )
                    val result = SmartResponse(
                        cached.data as T,
                        hasChanged = false,
                        fetchedAtMillis = cached.fetchedAtMillis
                    )
                    deferred.complete(result)
                    return result
                }

                error("HTTP request failed with status: ${response.status.value} for URL: $url")
            } catch (e: CancellationException) {
                deferred.completeExceptionally(e)
                throw e
            } catch (e: Exception) {
                if (cached != null && staleOnError) {
                    logger.warning(
                        "Request failed for $url (${e.message}), " +
                            "serving stale cache for key: $cacheKey"
                    )
                    val result = SmartResponse(
                        cached.data as T,
                        hasChanged = false,
                        fetchedAtMillis = cached.fetchedAtMillis
                    )
                    deferred.complete(result)
                    return result
                }

                deferred.completeExceptionally(e)
                throw e
            } catch (e: Throwable) {
                // Errors (not only Exceptions) must complete the deferred so that follower
                // coroutines waiting on the single-flight entry are not left hanging.
                deferred.completeExceptionally(e)
                throw e
            } finally {
                inFlight.remove(cacheKey, deferred)
            }
        }
    }
}
