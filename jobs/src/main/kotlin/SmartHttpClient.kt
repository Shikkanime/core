package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.ktor.createHttpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
    data class SmartResponse<T>(
        val data: T,
        val hasChanged: Boolean
    )

    data class HttpCacheEntry(
        val data: Any,
        val contentHash: Int,
        val etag: String?,
        val lastModified: String?,
        val expiresAtMillis: Long
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
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, HttpCacheEntry>?): Boolean =
                size > capacity
        }

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

        @Synchronized
        operator fun set(key: String, value: HttpCacheEntry) {
            cleanUpExpired(Clock.System.now().toEpochMilliseconds())
            map[key] = value
        }

        @Synchronized
        fun remove(key: String): HttpCacheEntry? = map.remove(key)

        @Synchronized
        fun clear() = map.clear()

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

    suspend inline fun <reified T : Any> get(
        url: String,
        cacheKey: String = url,
        ttl: Duration = Duration.ZERO,
        staleOnError: Boolean = true,
        headers: Map<String, String> = emptyMap()
    ): SmartResponse<T> {
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        // Return immediately if cached data is still fresh within its TTL
        val cached = cache[cacheKey]
        if (cached != null && nowMillis < cached.expiresAtMillis) {
            logger.fine("Cache hit (fresh) for key: $cacheKey")
            return SmartResponse(cached.data as T, hasChanged = false)
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
                val response = client.get(url) {
                    headers.forEach { (key, value) -> header(key, value) }

                    if (cached != null) {
                        cached.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                        cached.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
                    }
                }

                val now = Clock.System.now().toEpochMilliseconds()

                if (cached != null && response.status == HttpStatusCode.NotModified) {
                    logger.fine("Cache revalidated (304 Not Modified) for key: $cacheKey")
                    val updated = cached.copy(expiresAtMillis = now + ttl.inWholeMilliseconds)
                    cache[cacheKey] = updated
                    val result = SmartResponse(updated.data as T, hasChanged = false)
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
                        val result = SmartResponse(updated.data as T, hasChanged = false)
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
                        expiresAtMillis = now + ttl.inWholeMilliseconds
                    )

                    val result = SmartResponse(parsedData, hasChanged = true)
                    deferred.complete(result)
                    return result
                }

                if (cached != null && staleOnError) {
                    logger.warning("HTTP ${response.status.value} from $url, serving stale cache for key: $cacheKey")
                    val result = SmartResponse(cached.data as T, hasChanged = false)
                    deferred.complete(result)
                    return result
                }

                error("HTTP request failed with status: ${response.status.value} for URL: $url")
            } catch (e: Exception) {
                if (cached != null && staleOnError) {
                    logger.warning("Request failed for $url (${e.message}), serving stale cache for key: $cacheKey")
                    val result = SmartResponse(cached.data as T, hasChanged = false)
                    deferred.complete(result)
                    return result
                }

                deferred.completeExceptionally(e)
                throw e
            } finally {
                inFlight.remove(cacheKey, deferred)
            }
        }
    }
}
