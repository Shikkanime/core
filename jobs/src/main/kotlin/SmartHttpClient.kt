package fr.shikkanime.jobs

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.ktor.createHttpClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.koin.core.annotation.Single
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Smart HTTP client wrapper designed for scheduled jobs polling remote APIs.
 *
 * Provides:
 * - Content change detection via HTTP 304, ETags, Last-Modified, and body hash comparisons.
 * - Single-flight request coalescing to prevent concurrent duplicate network calls.
 * - Memory-bounded LRU cache with automatic eviction and stale expiration without third-party libraries.
 * - Raw body archiving of every changed GET response, to debug what a detected change actually
 *   carries when the downstream data ends up identical.
 */
@Single
class SmartHttpClient(
    val client: HttpClient = createHttpClient(),
    val maxCacheCapacity: Int = 1000,
    val maxStaleRetention: Duration = 24.hours,
    val responseDumpDir: File = File("logs/http-responses")
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
     * @param ttl the freshness duration of the cached payload; a whole number of minutes
     * expires on the next minute boundary instead of a rolling delay.
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
     * @param ttl the freshness duration of the cached payload; a whole number of minutes
     * expires on the next minute boundary instead of a rolling delay.
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
     * @param ttl the freshness duration of the cached payload; a whole number of minutes
     * expires on the next minute boundary instead of a rolling delay.
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
                    val updated = cached.copy(expiresAtMillis = expiryMillis(now, ttl))
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
                            expiresAtMillis = expiryMillis(now, ttl)
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

                    // Only GET bodies are archived: POST payloads may carry credentials
                    // (the Crunchyroll token endpoint, for example) that must not hit the disk.
                    if (httpMethod == HttpMethod.Get)
                        archiveResponseBody(cacheKey, url, bodyText)

                    cache[cacheKey] = HttpCacheEntry(
                        data = parsedData,
                        contentHash = contentHash,
                        etag = etag,
                        lastModified = lastModified,
                        expiresAtMillis = expiryMillis(now, ttl),
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

    /**
     * Archives the raw body of a changed GET response under [responseDumpDir].
     *
     * Each cache key gets its own directory holding two files: `latest.body` always carries the
     * most recently fetched body and `previous.body` the one it replaced, so a detected change
     * can be inspected with a plain diff. Every fetched body is also appended to a `history`
     * subdirectory, pruned to the [MAX_HISTORY_FILES] most recent ones, so successive changes
     * never erase the versions in between. When a previous body exists, the first difference
     * between the two is logged with a context excerpt of both sides.
     *
     * Cache keys embedding rotating identifiers (the batched CMS objects requests, for
     * example) would otherwise accumulate one directory per key combination forever, so the
     * dump directories whose last archived body is older than [DUMP_RETENTION] are pruned on
     * every archive.
     *
     * The archiving is best-effort: an IO failure is logged and never interrupts the fetch.
     *
     * @param cacheKey the cache key identifying the archived payload.
     * @param url the remote URL the body was fetched from.
     * @param bodyText the raw response body to archive.
     * @return nothing.
     * @see [responseDumpDir]
     */
    @PublishedApi
    internal fun archiveResponseBody(cacheKey: String, url: String, bodyText: String) {
        runCatching {
            val keyDir = dumpDirFor(cacheKey) ?: return@runCatching

            archiveLatestVersion(keyDir, cacheKey, url, bodyText)
            val timestamp = appendToHistory(keyDir, bodyText)

            pruneStaleDumpDirs(timestamp)
        }.onFailure { e ->
            logger.warning("Failed to archive the response body of '$cacheKey': ${e.message}")
        }
    }

    /**
     * Resolves the dump directory of a cache key, creating it when missing.
     *
     * The hash suffix keeps distinct keys in distinct directories even when their sanitized
     * names collide or truncate to the same prefix.
     *
     * @param cacheKey the cache key identifying the archived payload.
     * @return the dump directory of the key, or `null` when it cannot be created.
     * @see [archiveResponseBody]
     */
    private fun dumpDirFor(cacheKey: String): File? {
        val keyDir = File(
            responseDumpDir,
            cacheKey.replace(ILLEGAL_FILENAME_CHARS, "_")
                .take(MAX_DUMP_KEY_LENGTH - KEY_HASH_SUFFIX_LENGTH) +
                "-${cacheKey.hashCode().toUInt().toString(16)}"
        )
        return keyDir.takeIf { it.isDirectory || it.mkdirs() || it.isDirectory }
    }

    /**
     * Rotates the archived body of a key into `previous.body`, logs the first difference with
     * the body just fetched, then writes it as `latest.body`.
     *
     * @param keyDir the dump directory of the key.
     * @param cacheKey the cache key identifying the archived payload.
     * @param url the remote URL the body was fetched from.
     * @param bodyText the raw response body to archive.
     * @return nothing.
     * @see [archiveResponseBody]
     * @see [logBodyDifference]
     */
    private fun archiveLatestVersion(keyDir: File, cacheKey: String, url: String, bodyText: String) {
        val latest = File(keyDir, "latest.body")
        val previousBody = latest.takeIf { it.isFile }?.readText()
        if (previousBody != null) {
            File(keyDir, "previous.body").writeText(previousBody)
            logBodyDifference(cacheKey, url, previousBody, bodyText)
        }
        latest.writeText(bodyText)
    }

    /**
     * Appends a body to the `history` subdirectory of a dump directory and prunes it to the
     * [MAX_HISTORY_FILES] most recent files.
     *
     * @param keyDir the dump directory of the key.
     * @param bodyText the raw response body to archive.
     * @return the timestamp in epoch milliseconds used for the history file name.
     * @see [archiveResponseBody]
     */
    private fun appendToHistory(keyDir: File, bodyText: String): Long {
        val historyDir = File(keyDir, "history")
        historyDir.mkdirs()
        val timestamp = Clock.System.now().toEpochMilliseconds()
        var historyFile = File(historyDir, "$timestamp.body")
        var collision = 1
        while (historyFile.exists()) {
            historyFile = File(historyDir, "$timestamp-$collision.body")
            collision++
        }
        historyFile.writeText(bodyText)
        historyDir.listFiles()
            ?.sortedByDescending {
                it.nameWithoutExtension.substringBefore('-').toLongOrNull() ?: 0L
            }
            ?.drop(MAX_HISTORY_FILES)
            ?.forEach(File::delete)
        return timestamp
    }

    /**
     * Deletes the dump directories whose most recent archived body is older than [DUMP_RETENTION].
     *
     * @param nowMillis the reference timestamp in epoch milliseconds.
     * @return nothing.
     * @see [archiveResponseBody]
     */
    private fun pruneStaleDumpDirs(nowMillis: Long) {
        runCatching {
            responseDumpDir.listFiles()
                ?.filter(File::isDirectory)
                ?.filter {
                    val latest = File(it, "latest.body")
                    val lastModified =
                        if (latest.isFile)
                            latest.lastModified()
                        else
                            it.lastModified()
                    nowMillis - lastModified > DUMP_RETENTION.inWholeMilliseconds
                }
                ?.forEach(File::deleteRecursively)
        }.onFailure { e ->
            logger.warning("Failed to prune stale response dumps: ${e.message}")
        }
    }

    /**
     * Logs the position of the first difference between two versions of a response body.
     *
     * @param cacheKey the cache key identifying the payload.
     * @param url the remote URL the body was fetched from.
     * @param previous the body archived before this fetch.
     * @param current the body just fetched.
     * @return nothing.
     * @see [archiveResponseBody]
     */
    private fun logBodyDifference(cacheKey: String, url: String, previous: String, current: String) {
        val firstDifference = previous.commonPrefixWith(current).length
        val context = 120
        val previousExcerpt = previous.substring(
            maxOf(0, firstDifference - context),
            minOf(previous.length, firstDifference + context)
        )
        val currentExcerpt = current.substring(
            maxOf(0, firstDifference - context),
            minOf(current.length, firstDifference + context)
        )
        logger.info(
            "Response body changed for key '$cacheKey' ($url): first difference at char " +
                "$firstDifference (previous ${previous.length} chars, current ${current.length} chars)\n" +
                "  previous: …$previousExcerpt…\n" +
                "  current : …$currentExcerpt…"
        )
    }

    /**
     * Computes the expiration timestamp of a cache entry for the given TTL.
     *
     * A TTL made of whole minutes expires on the minute boundary that many minutes after the
     * current one, so every platform refreshes its catalog at the start of a minute — before
     * the harvest persists the episodes. Any other TTL keeps the rolling `now + ttl` delay.
     *
     * @param nowMillis the reference timestamp in epoch milliseconds.
     * @param ttl the freshness duration of the cached payload.
     * @return the expiration timestamp in epoch milliseconds.
     * @see [cachedRequest]
     */
    @PublishedApi
    internal fun expiryMillis(nowMillis: Long, ttl: Duration): Long {
        val ttlMillis = ttl.inWholeMilliseconds
        if (ttlMillis > 0 && ttlMillis % MINUTE_MILLIS == 0L) {
            return (nowMillis / MINUTE_MILLIS + ttlMillis / MINUTE_MILLIS) * MINUTE_MILLIS
        }
        return nowMillis + ttlMillis
    }

    private companion object {
        const val MAX_DUMP_KEY_LENGTH = 200
        const val MINUTE_MILLIS = 60_000L
        const val MAX_HISTORY_FILES = 10
        const val KEY_HASH_SUFFIX_LENGTH = 9 // '-' + 8 hexadecimal digits of the key hash
        val DUMP_RETENTION = 7.days
        val ILLEGAL_FILENAME_CHARS = Regex("[^A-Za-z0-9._-]")
    }
}
