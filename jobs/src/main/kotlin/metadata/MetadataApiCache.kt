package fr.shikkanime.jobs.metadata

import fr.shikkanime.database.services.ApiCacheService
import fr.shikkanime.models.ApiCacheEntry
import fr.shikkanime.models.MetadataPlatform
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Persistent cache of external metadata API responses.
 *
 * Every payload is stored raw under a key identifying the request, and a fresh entry answers
 * the request without any network call: the platforms behind these APIs ban aggressive
 * clients, so a repeated query must never reach them twice inside the TTL window.
 *
 * @param apiCacheService persistence of the cached payloads.
 */
@Single
class MetadataApiCache(
    private val apiCacheService: ApiCacheService
) {
    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns the cached body of [key] while it is fresher than [ttl], fetching and storing it
     * otherwise.
     *
     * @param key identifier of the request, built by the caller.
     * @param ttl freshness window of the cached payload.
     * @param fetch suspending block performing the request when the cache misses.
     * @return the response body, from the cache or from [fetch].
     */
    suspend fun getOrPut(key: String, ttl: Duration, fetch: suspend () -> String): String {
        apiCacheService.findByKey(key)?.let { entry ->
            val createdAt = entry.createdAt?.toInstant(TimeZone.UTC)?.toEpochMilliseconds()
            if (createdAt != null &&
                Clock.System.now().toEpochMilliseconds() - createdAt < ttl.inWholeMilliseconds
            )
                return entry.body
        }

        val body = fetch()
        apiCacheService.save(ApiCacheEntry(null, null, null, key, body))
        return body
    }

    /**
     * Builds a cache key from the platform, the endpoint name and its parameters.
     *
     * @param platform metadata platform owning the request.
     * @param endpoint name of the endpoint or operation.
     * @param params request parameters, joined in order.
     * @return the cache key to store the payload under.
     */
    fun key(platform: MetadataPlatform, endpoint: String, vararg params: String): String =
        buildString {
            append(platform.name.lowercase())
            append(':')
            append(endpoint)
            params.forEach { param ->
                append(':')
                append(param.lowercase().replace(Regex("\\s+"), "_").take(180))
            }
        }.let { key ->
            // Keep the key under the column limit even for long search queries
            if (key.length <= 512)
                key
            else
                key.take(400) + ":" + sha256(key)
        }

    /**
     * Hashes a cache key to a short hexadecimal fingerprint.
     *
     * @param value cache key to fingerprint.
     * @return the first 16 hexadecimal characters of its SHA-256 digest.
     */
    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

    /**
     * Decodes a cached JSON body.
     *
     * @param T type of the expected payload.
     * @param body raw JSON body.
     * @return the decoded payload.
     */
    inline fun <reified T> decode(body: String): T =
        json.decodeFromString(body)
}
