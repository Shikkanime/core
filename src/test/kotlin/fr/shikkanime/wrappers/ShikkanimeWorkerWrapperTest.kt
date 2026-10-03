package fr.shikkanime.wrappers

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.system.CircuitBreaker
import fr.shikkanime.wrappers.factories.AbstractShikkanimeWorkerWrapper
import fr.shikkanime.wrappers.impl.ShikkanimeWorkerWrapper
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ShikkanimeWorkerWrapperTest {
    private val episodes = listOf(
        AbstractShikkanimeWorkerWrapper.Episode(1, listOf("ja-JP", "fr-FR")),
        AbstractShikkanimeWorkerWrapper.Episode(2, listOf("ja-JP"))
    )

    @BeforeEach
    fun setUp() {
        ShikkanimeWorkerWrapper.circuitBreaker.reset()
        mockkObject(HttpRequest)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        ShikkanimeWorkerWrapper.circuitBreaker.reset()
    }

    private fun mockSuccessResponse(): HttpResponse {
        val call = mockk<HttpClientCall>()
        coEvery { call.bodyNullable(any()) } returns episodes

        val response = mockk<HttpResponse>()
        every { response.status } returns HttpStatusCode.OK
        every { response.call } returns call
        return response
    }

    private fun mockErrorResponse(status: HttpStatusCode): HttpResponse {
        mockkStatic(HttpResponse::bodyAsText)
        val response = mockk<HttpResponse>()
        every { response.status } returns status
        coEvery { response.bodyAsText(any()) } returns "Internal error"
        return response
    }

    private fun mockTimeout() {
        coEvery { HttpRequest.post(any(), any(), any(), any()) } throws HttpRequestTimeoutException("https://worker.shikkanime.fr/netflix-episodes", 100_000)
    }

    private fun getNetflixEpisodes() = runBlocking {
        ShikkanimeWorkerWrapper.getNetflixEpisodes("netflixId", "secureNetflixId", 1, 2)
    }

    @Test
    fun `should return episodes when the service responds`() {
        val response = mockSuccessResponse()
        coEvery { HttpRequest.post(any(), any(), any(), any()) } returns response

        assertEquals(episodes, getNetflixEpisodes())
        assertEquals(CircuitBreaker.State.CLOSED, ShikkanimeWorkerWrapper.circuitBreaker.getState())
        coVerify(exactly = 1) { HttpRequest.post(any(), any(), any(), any()) }
    }

    @Test
    fun `should throw when the service does not respond`() {
        mockTimeout()

        assertThrows<ShikkanimeWorkerWrapper.ServiceUnavailableException> { getNetflixEpisodes() }
        assertTrue(ShikkanimeWorkerWrapper.circuitBreaker.isAvailable())
    }

    @Test
    fun `should open the circuit after reaching the failure threshold`() {
        mockTimeout()

        repeat(ShikkanimeWorkerWrapper.FAILURE_THRESHOLD) {
            assertThrows<ShikkanimeWorkerWrapper.ServiceUnavailableException> { getNetflixEpisodes() }
        }

        assertFalse(ShikkanimeWorkerWrapper.circuitBreaker.isAvailable())
        assertEquals(CircuitBreaker.State.OPEN, ShikkanimeWorkerWrapper.circuitBreaker.getState())
    }

    @Test
    fun `should not call the service anymore when the circuit is open`() {
        mockTimeout()

        repeat(ShikkanimeWorkerWrapper.FAILURE_THRESHOLD) {
            assertThrows<ShikkanimeWorkerWrapper.ServiceUnavailableException> { getNetflixEpisodes() }
        }

        // Even if the service is back, the circuit stays open until the recovery timeout
        val response = mockSuccessResponse()
        coEvery { HttpRequest.post(any(), any(), any(), any()) } returns response

        repeat(5) {
            assertThrows<ShikkanimeWorkerWrapper.ServiceUnavailableException> { getNetflixEpisodes() }
        }

        coVerify(exactly = ShikkanimeWorkerWrapper.FAILURE_THRESHOLD) { HttpRequest.post(any(), any(), any(), any()) }
    }

    @Test
    fun `should count non OK responses as failures`() {
        val response = mockErrorResponse(HttpStatusCode.InternalServerError)
        coEvery { HttpRequest.post(any(), any(), any(), any()) } returns response

        repeat(ShikkanimeWorkerWrapper.FAILURE_THRESHOLD) {
            assertThrows<ShikkanimeWorkerWrapper.ServiceUnavailableException> { getNetflixEpisodes() }
        }

        assertFalse(ShikkanimeWorkerWrapper.circuitBreaker.isAvailable())
    }

    @Test
    fun `should reset the failure count after a successful call`() {
        val successResponse = mockSuccessResponse()

        repeat(2) {
            mockTimeout()

            repeat(ShikkanimeWorkerWrapper.FAILURE_THRESHOLD - 1) {
                assertThrows<ShikkanimeWorkerWrapper.ServiceUnavailableException> { getNetflixEpisodes() }
            }

            coEvery { HttpRequest.post(any(), any(), any(), any()) } returns successResponse
            assertEquals(episodes, getNetflixEpisodes())
        }

        assertEquals(CircuitBreaker.State.CLOSED, ShikkanimeWorkerWrapper.circuitBreaker.getState())
    }
}
