package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.system.CircuitBreaker
import fr.shikkanime.wrappers.factories.AbstractShikkanimeWorkerWrapper
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.Duration

object ShikkanimeWorkerWrapper : AbstractShikkanimeWorkerWrapper() {
    const val FAILURE_THRESHOLD = 3
    val RECOVERY_TIMEOUT: Duration = Duration.ofMinutes(30)

    internal val circuitBreaker = CircuitBreaker("ShikkanimeWorker", FAILURE_THRESHOLD, RECOVERY_TIMEOUT)

    class ServiceUnavailableException : IllegalStateException("Shikkanime worker is unavailable")

    override suspend fun getNetflixEpisodes(
        netflixId: String,
        secureNetflixId: String,
        vararg ids: Int,
        bypass: Boolean
    ): List<Episode> =
        circuitBreaker.execute(
            action = {
                val response = HttpRequest.post(
                    "$baseUrl/netflix-episodes${if (bypass) "?bypass=true" else ""}",
                    headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
                    timeout = if (bypass) 300_000 else 100_000,
                    body = Request(
                        ids = ids.toList(),
                        netflixId = netflixId,
                        secureNetflixId = secureNetflixId
                    )
                )
                require(response.status == HttpStatusCode.OK) { "Failed to get episodes (${response.status.value} - ${response.bodyAsText()})" }
                response.body<List<Episode>>()
            },
            // Callers already handle failures (default audio locale), so we keep throwing
            // instead of returning a misleading empty result
            fallback = { throw ServiceUnavailableException() }
        )
}
