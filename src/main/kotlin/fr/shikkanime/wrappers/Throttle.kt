package fr.shikkanime.wrappers

import fr.shikkanime.utils.LoggerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

open class Throttle(val rateLimit: Int) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val mutex = Mutex()
    private var nextRequestTime = 0L

    private suspend fun throttle() {
        require(rateLimit > 0) { "rateLimit must be greater than 0" }

        val waitTime = mutex.withLock {
            val now = System.currentTimeMillis()
            val minInterval = 60_000L / rateLimit
            val scheduledRequestTime = max(now, nextRequestTime)

            nextRequestTime = scheduledRequestTime + minInterval

            scheduledRequestTime - now
        }

        if (waitTime > 0) {
            logger.config("Throttling request for platform ${javaClass.simpleName}. Wait time: $waitTime ms")
            delay(waitTime.milliseconds)
        }
    }

    protected suspend fun <T> execute(block: suspend () -> T): T {
        throttle()
        return block()
    }
}