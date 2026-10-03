package fr.shikkanime.utils.system

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class CircuitBreakerTest {

    private lateinit var circuitBreaker: CircuitBreaker
    private val failureThreshold = 3
    private val recoveryTimeout = Duration.ofMillis(100)

    @BeforeEach
    fun setUp() {
        circuitBreaker = CircuitBreaker("test", failureThreshold, recoveryTimeout)
    }

    private fun trip() {
        repeat(failureThreshold) {
            circuitBreaker.execute(action = { throw RuntimeException("Failure") }, fallback = { "FALLBACK" })
        }
    }

    @Test
    fun `should be in CLOSED state initially`() {
        assertTrue(circuitBreaker.isAvailable())
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState())
    }

    @Test
    fun `should remain in CLOSED state after successful calls`() {
        val result = circuitBreaker.execute(action = { "SUCCESS" }, fallback = { "FALLBACK" })
        assertEquals("SUCCESS", result)
        assertTrue(circuitBreaker.isAvailable())
    }

    @Test
    fun `should move to OPEN state after reaching failure threshold`() {
        val exception = RuntimeException("Service Unavailable")

        repeat(failureThreshold) {
            val result = circuitBreaker.execute(action = { throw exception }, fallback = { "FALLBACK" })
            assertEquals("FALLBACK", result)
        }

        assertFalse(circuitBreaker.isAvailable())
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState())
    }

    @Test
    fun `should execute fallback immediately when in OPEN state`() {
        trip()
        assertFalse(circuitBreaker.isAvailable())

        // Attempt another call
        val actionCalls = AtomicInteger(0)
        val result = circuitBreaker.execute(action = { actionCalls.incrementAndGet(); "SUCCESS" }, fallback = { "FALLBACK" })
        assertEquals("FALLBACK", result)
        assertEquals(0, actionCalls.get())
    }

    @Test
    fun `should transition to HALF_OPEN state after recovery timeout`() {
        trip()
        assertFalse(circuitBreaker.isAvailable())

        // Wait for the recovery timeout
        Thread.sleep(recoveryTimeout.toMillis() + 10)

        // The next call should be in HALF_OPEN state. Let's make it fail.
        val result = circuitBreaker.execute(action = { throw RuntimeException("Failure in HALF_OPEN") }, fallback = { "FALLBACK" })
        assertEquals("FALLBACK", result)
        assertFalse(circuitBreaker.isAvailable()) // Should go back to OPEN
    }

    @Test
    fun `should transition to CLOSED state from HALF_OPEN after a successful call`() {
        trip()
        assertFalse(circuitBreaker.isAvailable())

        // Wait for the recovery timeout
        Thread.sleep(recoveryTimeout.toMillis() + 10)

        // The next call should be in HALF_OPEN state. Let's make it succeed.
        val result = circuitBreaker.execute(action = { "SUCCESS" }, fallback = { "FALLBACK" })
        assertEquals("SUCCESS", result)
        assertTrue(circuitBreaker.isAvailable()) // Should be back to CLOSED
    }

    @Test
    fun `should support suspending action and fallback`() = runBlocking {
        val result = circuitBreaker.execute(
            action = {
                delay(1)
                "SUCCESS"
            },
            fallback = {
                delay(1)
                "FALLBACK"
            }
        )
        assertEquals("SUCCESS", result)

        val fallbackResult = circuitBreaker.execute(
            action = {
                delay(1)
                throw RuntimeException("Failure")
            },
            fallback = {
                delay(1)
                "FALLBACK"
            }
        )
        assertEquals("FALLBACK", fallbackResult)
    }

    @Test
    fun `should rethrow CancellationException without counting it as a failure`() {
        repeat(failureThreshold) {
            assertThrows<CancellationException> {
                circuitBreaker.execute(action = { throw CancellationException("Cancelled") }, fallback = { "FALLBACK" })
            }
        }

        assertTrue(circuitBreaker.isAvailable())
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState())
    }

    @Test
    fun `should propagate exception thrown by the fallback`() {
        assertThrows<IllegalStateException> {
            circuitBreaker.execute(action = { throw RuntimeException("Failure") }, fallback = { throw IllegalStateException("Unavailable") })
        }

        trip()

        assertThrows<IllegalStateException> {
            circuitBreaker.execute(action = { "SUCCESS" }, fallback = { throw IllegalStateException("Unavailable") })
        }
    }

    @Test
    fun `should be CLOSED after a manual reset`() {
        trip()
        assertFalse(circuitBreaker.isAvailable())

        circuitBreaker.reset()

        assertTrue(circuitBreaker.isAvailable())
        assertEquals("SUCCESS", circuitBreaker.execute(action = { "SUCCESS" }, fallback = { "FALLBACK" }))
    }

    @Test
    fun `should handle concurrent calls correctly`() {
        val successCounter = AtomicInteger(0)
        val fallbackCounter = AtomicInteger(0)
        val exception = RuntimeException("Service Unavailable")

        val threads = List(10) {
            Thread {
                try {
                    val result = circuitBreaker.execute(
                        action = {
                            if (successCounter.get() < 5) {
                                throw exception
                            }
                            "SUCCESS"
                        },
                        fallback = { "FALLBACK" }
                    )

                    if (result == "SUCCESS") {
                        successCounter.incrementAndGet()
                    } else {
                        fallbackCounter.incrementAndGet()
                    }
                } catch (_: Exception) {
                    // Should not happen as fallback handles it
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // The exact numbers can vary due to thread scheduling, but we expect some fallbacks.
        assertTrue(fallbackCounter.get() > 0)
    }
}
