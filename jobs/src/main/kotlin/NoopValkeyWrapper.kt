package fr.shikkanime.jobs

import fr.shikkanime.cache.ValkeyWrapper
import kotlin.time.Duration

/**
 * Cache client used when Valkey is unreachable from the job.
 *
 * The harvest writes the database; a cache outage must not fail the run. Every lookup misses and
 * every invalidation is a no-op, so the API entries stay until their TTL expires.
 */
internal object NoopValkeyWrapper : ValkeyWrapper {
    override suspend fun get(key: String): ByteArray? =
        null

    override suspend fun set(key: String, value: ByteArray, ttl: Duration) =
        Unit

    override suspend fun incr(key: String): Long =
        0
}
