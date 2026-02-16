package fr.shikkanime.utils

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.CancellationException

private val logger = LoggerFactory.getLogger(HttpRequest::class.java)

class HttpRequest(private val timeout: Long = 60_000) {
    private fun httpClient() = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        engine {
            config {
                followRedirects(true)
            }
        }
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        logger.info("Making request to $url... (GET)")
        val start = System.currentTimeMillis()

        val response = httpClient().use {
            it.get(url) {
                headers.forEach(::header)
            }
        }

        logger.info("Request to $url done in ${System.currentTimeMillis() - start}ms (GET)")
        return response
    }

    suspend fun post(url: String, headers: Map<String, String> = emptyMap(), body: Any? = null): HttpResponse {
        logger.info("Making request to $url... (POST)")
        val start = System.currentTimeMillis()

        val response = httpClient().use {
            if (body is List<*> && body.all { element -> element is PartData }) {
                @Suppress("UNCHECKED_CAST")
                it.submitFormWithBinaryData(url, body as List<PartData>) {
                    headers.forEach(::header)
                }
            } else {
                it.post(url) {
                    headers.forEach(::header)
                    body?.let(::setBody)
                }
            }
        }

        logger.info("Request to $url done in ${System.currentTimeMillis() - start}ms (POST)")
        return response
    }

    suspend fun getCookies(url: String): Pair<Document, Map<String, String>> {
        val response = get(url)
        return Jsoup.parse(response.bodyAsText()) to response.setCookie().associate { it.name to it.value }
    }

    companion object {
        suspend fun <T> retry(
            times: Int,
            delay: Long = 500,
            shouldRetry: (Exception) -> Boolean = { true },
            operation: suspend () -> T
        ): T {
            var lastException: Exception? = null

            repeat(times) { attempt ->
                try {
                    return operation()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    if (!shouldRetry(e)) throw e

                    lastException = e
                    val attemptNum = attempt + 1

                    logger.warning("Attempt $attemptNum failed: ${e.message}")

                    if (attempt < times - 1) {
                        logger.warning("Retrying in $delay ms...")
                        delay(delay)
                    }
                }
            }

            throw lastException ?: Exception("Failed after $times attempts")
        }

        suspend fun <T> retryOnTimeout(
            times: Int,
            delay: Long = 500,
            operation: suspend () -> T
        ) = retry(
            times = times,
            delay = delay,
            shouldRetry = { it is HttpRequestTimeoutException },
            operation = operation
        )
    }
}
