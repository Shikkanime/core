package fr.shikkanime.utils.system

import fr.shikkanime.utils.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
    private var lastFailureTime: Instant? = null

    fun isAvailable(): Boolean {
        return state.get() != State.OPEN
    }

    fun <T> execute(action: () -> T, fallback: () -> T): T {
        return when (state.get()) {
            State.CLOSED -> {
                try {
                    val result = action()
                    reset()
                    result
                } catch (e: Exception) {
                    recordFailure()
                    logger.warning("CircuitBreaker[$name]: Service call failed. ${e.message}")
                    fallback()
                }
            }
            State.OPEN -> {
                if (lastFailureTime != null && Instant.now()
                        .isAfter(lastFailureTime!!.plus(recoveryTimeout))
                ) {
                    logger.info("CircuitBreaker[$name]: State changing to HALF_OPEN")
                    state.set(State.HALF_OPEN)
                    execute(action, fallback)
                } else {
                    logger.warning("CircuitBreaker[$name]: Service is unavailable. Executing fallback.")
                    fallback()
                }
            }
            State.HALF_OPEN -> {
                try {
                    val result = action()
                    reset()
                    logger.info("CircuitBreaker[$name]: Service has recovered. State changing to CLOSED.")
                    result
                } catch (e: Exception) {
                    trip()
                    logger.warning("CircuitBreaker[$name]: Service call failed in HALF_OPEN state. ${e.message}")
                    fallback()
                }
            }
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

    private fun reset() {
        state.set(State.CLOSED)
        failureCount.set(0)
        lastFailureTime = null
    }
}

