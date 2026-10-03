package fr.shikkanime.utils.system

import fr.shikkanime.utils.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import kotlin.coroutines.cancellation.CancellationException

class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int,
    private val recoveryTimeout: Duration
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    enum class State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private val state = AtomicReference(State.CLOSED)
    private val failureCount = AtomicInteger(0)
    @Volatile
    private var lastFailureTime: Instant? = null

    fun isAvailable(): Boolean {
        return state.get() != State.OPEN
    }

    fun getState(): State = state.get()

    /**
     * Execute the [action] protected by the circuit breaker, or the [fallback] if the circuit is open
     * or if the [action] fails.
     *
     * This function is `inline` so that both [action] and [fallback] can call suspending functions
     * when used from a coroutine.
     */
    inline fun <T> execute(action: () -> T, fallback: () -> T): T {
        if (!tryAcquirePermission()) return fallback()

        return try {
            val result = action()
            onSuccess()
            result
        } catch (e: CancellationException) {
            // A cancelled coroutine is not a failure of the remote service
            throw e
        } catch (e: Exception) {
            onFailure(e)
            fallback()
        }
    }

    @PublishedApi
    internal fun tryAcquirePermission(): Boolean {
        return when (state.get()) {
            State.CLOSED, State.HALF_OPEN -> true
            State.OPEN -> {
                val lastFailure = lastFailureTime

                if (lastFailure != null && Instant.now().isAfter(lastFailure.plus(recoveryTimeout))) {
                    logger.info("CircuitBreaker[$name]: State changing to HALF_OPEN")
                    state.set(State.HALF_OPEN)
                    true
                } else {
                    logger.warning("CircuitBreaker[$name]: Service is unavailable. Executing fallback.")
                    false
                }
            }
        }
    }

    @PublishedApi
    internal fun onSuccess() {
        val previousState = state.get()
        reset()

        if (previousState == State.HALF_OPEN)
            logger.info("CircuitBreaker[$name]: Service has recovered. State changing to CLOSED.")
    }

    @PublishedApi
    internal fun onFailure(e: Exception) {
        if (state.get() == State.HALF_OPEN) {
            trip()
            logger.warning("CircuitBreaker[$name]: Service call failed in HALF_OPEN state. ${e.message}")
        } else {
            recordFailure()
            logger.log(Level.WARNING, "CircuitBreaker[$name]: Service call failed.", e)
        }
    }

    private fun recordFailure() {
        val currentFailures = failureCount.incrementAndGet()
        if (currentFailures >= failureThreshold) {
            trip()
        } else {
            lastFailureTime = Instant.now()
        }
    }

    private fun trip() {
        logger.warning("CircuitBreaker[$name]: Tripping the circuit. State changing to OPEN.")
        state.set(State.OPEN)
        lastFailureTime = Instant.now()
    }

    fun reset() {
        state.set(State.CLOSED)
        failureCount.set(0)
        lastFailureTime = null
    }
}
