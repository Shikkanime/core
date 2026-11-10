package fr.shikkanime.jobs

import fr.shikkanime.jobs.builders.impl.HttpCacheEntryMockKBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@ExtendWith(MockKExtension::class)
@DisplayName("tests for SmartHttpClient")
class SmartHttpClientTest {

    @Nested
    @DisplayName("tests for LruMemoryCache")
    inner class LruMemoryCacheTests {

        @Test
        @DisplayName("should evict eldest entry when maxCacheCapacity is reached")
        fun `should evict eldest entry when maxCacheCapacity is reached`() {
            // Given
            val client = SmartHttpClient(
                maxCacheCapacity = 2,
                maxStaleRetention = 10.minutes
            )
            val now = Clock.System.now()
                .toEpochMilliseconds()
            val entry1 = HttpCacheEntryMockKBuilder {
                data = "data1"
                contentHash = 1
                expiresAtMillis = now + 10000
            }.build()
            val entry2 = HttpCacheEntryMockKBuilder {
                data = "data2"
                contentHash = 2
                expiresAtMillis = now + 10000
            }.build()
            val entry3 = HttpCacheEntryMockKBuilder {
                data = "data3"
                contentHash = 3
                expiresAtMillis = now + 10000
            }.build()

            client.cache["key1"] = entry1
            client.cache["key2"] = entry2

            // When
            client.cache["key1"]
            client.cache["key3"] = entry3

            // Then
            assertEquals(2, client.cache.size)
            assertEquals(entry1, client.cache["key1"])
            assertNull(client.cache["key2"])
            assertEquals(entry3, client.cache["key3"])
        }

        @Test
        @DisplayName("should purge entry expired beyond maxStaleRetention")
        fun `should purge entry expired beyond maxStaleRetention`() {
            // Given
            val client = SmartHttpClient(
                maxCacheCapacity = 10,
                maxStaleRetention = 100.milliseconds
            )
            val now = Clock.System.now()
                .toEpochMilliseconds()
            val expiredEntry = HttpCacheEntryMockKBuilder {
                data = "expired"
                contentHash = 123
                expiresAtMillis = now - 200
            }.build()

            // When
            client.cache["expiredKey"] = expiredEntry

            // Then
            assertNull(client.cache["expiredKey"])
            assertEquals(0, client.cache.size)
        }

        @Test
        @DisplayName("should keep entry within maxStaleRetention even if TTL expired")
        fun `should keep entry within maxStaleRetention even if TTL expired`() {
            // Given
            val client = SmartHttpClient(
                maxCacheCapacity = 10,
                maxStaleRetention = 2.hours
            )
            val now = Clock.System.now()
                .toEpochMilliseconds()
            val staleEntry = HttpCacheEntryMockKBuilder {
                data = "staleData"
                contentHash = 456
                etag = "\"v1\""
                expiresAtMillis = now - 10.minutes.inWholeMilliseconds
            }.build()

            // When
            client.cache["staleKey"] = staleEntry

            // Then
            assertEquals(staleEntry, client.cache["staleKey"])
            assertEquals(1, client.cache.size)
        }
    }

    @Nested
    @DisplayName("tests for the post method")
    inner class PostTests {

        @Test
        @DisplayName("should send the body with its content type and cache the response")
        fun `should send the body with its content type and cache the response`() = runTest {
            // Given
            var requests = 0
            var captured: HttpRequestData? = null
            val client = SmartHttpClient(
                client = HttpClient(
                    MockEngine { request ->
                        requests++
                        captured = request
                        respond("pong")
                    }
                )
            )

            // When
            val first = client.post<String>(
                url = "https://example.test/token",
                body = "grant_type=client_id",
                cacheKey = "test:post",
                ttl = 1.minutes,
                contentType = ContentType.Application.FormUrlEncoded
            )
            val second = client.post<String>(
                url = "https://example.test/token",
                body = "grant_type=client_id",
                cacheKey = "test:post",
                ttl = 1.minutes,
                contentType = ContentType.Application.FormUrlEncoded
            )

            // Then: a single request carried the form body, the second call was a cache hit
            val request = requireNotNull(captured)
            assertEquals(1, requests)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("grant_type=client_id", (request.body as TextContent).text)
            assertEquals(ContentType.Application.FormUrlEncoded, (request.body as TextContent).contentType)
            assertEquals("pong", first.data)
            assertFalse(second.hasChanged)
            assertTrue(first.hasChanged)
        }

        @Test
        @DisplayName("should serialize a non string body through the content negotiation")
        fun `should serialize a non string body through the content negotiation`() = runTest {
            // Given
            var captured: HttpRequestData? = null
            val client = SmartHttpClient(
                client = HttpClient(
                    MockEngine { request ->
                        captured = request
                        respond("{}")
                    }
                ) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
            )

            // When
            client.post<String>(
                url = "https://example.test/api",
                body = TokenRequestBody("client_id"),
                cacheKey = "test:post:json",
                contentType = ContentType.Application.Json
            )

            // Then: the body went through the JSON serializer, not the text wrapper
            val request = requireNotNull(captured)
            assertTrue(request.body is OutgoingContent.ByteArrayContent)
            assertEquals(ContentType.Application.Json, request.body.contentType)
        }
    }

    @Serializable
    private data class TokenRequestBody(val grantType: String)
}
