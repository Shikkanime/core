package fr.shikkanime.jobs.metadata

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Per-client request throttle.
 *
 * Metadata platforms ban aggressive clients, so every request waits until [minIntervalMillis]
 * elapsed since the previous one. The limiter is shared by the whole client, which makes
 * concurrent callers queue instead of bursting.
 *
 * @param requestsPerMinute maximum number of requests per minute allowed by the platform.
 */
class RateLimiter(requestsPerMinute: Int) {
    private val minIntervalMillis = 60_000L / requestsPerMinute
    private val mutex = Mutex()
    private var lastRequestAtMillis = 0L

    /**
     * Suspends until the next request slot is available, then reserves it.
     */
    suspend fun throttle() {
        mutex.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestAtMillis
            val wait = minIntervalMillis - elapsed
            if (wait > 0)
                delay(wait.milliseconds)
            lastRequestAtMillis = System.currentTimeMillis()
        }
    }
}
