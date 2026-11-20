package fr.shikkanime.jobs

import fr.shikkanime.jobs.builders.impl.HttpCacheEntryMockKBuilder
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    @DisplayName("tests for the get method")
    inner class GetTests {

        @Test
        @DisplayName("should revalidate an expired entry and report it unchanged")
        fun `should revalidate an expired entry and report it unchanged`() = runTest {
            // Given
            var calls = 0
            val client = SmartHttpClient(
                client = HttpClient(
                    MockEngine {
                        calls++
                        if (calls == 1) {
                            respond(
                                "v1",
                                headers = headersOf(HttpHeaders.ETag, "\"e1\"")
                            )
                        } else {
                            respond("", status = HttpStatusCode.NotModified)
                        }
                    }
                ),
                responseDumpDir = File("build/test-http-dumps")
            )

            // When: the zero ttl makes the entry stale immediately, forcing a revalidation
            val first = client.get<String>(
                url = "https://example.test/data",
                cacheKey = "test:get:etag",
                ttl = 0.minutes
            )
            val second = client.get<String>(
                url = "https://example.test/data",
                cacheKey = "test:get:etag",
                ttl = 0.minutes
            )

            // Then: the 304 kept the cached payload and its original fetch timestamp
            assertEquals(2, calls)
            assertTrue(first.hasChanged)
            assertFalse(second.hasChanged)
            assertEquals("v1", second.data)
            assertEquals(first.fetchedAtMillis, second.fetchedAtMillis)
        }

        @Test
        @DisplayName("should serve the stale entry when the request fails")
        fun `should serve the stale entry when the request fails`() = runTest {
            // Given
            var calls = 0
            val client = SmartHttpClient(
                client = HttpClient(
                    MockEngine {
                        calls++
                        if (calls == 1) {
                            respond("v1")
                        } else {
                            respond("boom", status = HttpStatusCode.InternalServerError)
                        }
                    }
                ),
                responseDumpDir = File("build/test-http-dumps")
            )

            // When
            client.get<String>(
                url = "https://example.test/data",
                cacheKey = "test:get:stale",
                ttl = 0.minutes
            )
            val stale = client.get<String>(
                url = "https://example.test/data",
                cacheKey = "test:get:stale",
                ttl = 0.minutes
            )

            // Then
            assertEquals(2, calls)
            assertFalse(stale.hasChanged)
            assertEquals("v1", stale.data)
            assertTrue(stale.fetchedAtMillis != null)
        }

        @Test
        @DisplayName("should archive the replaced and the new body of a changed GET response")
        fun `should archive the replaced and the new body of a changed GET response`() = runTest {
            // Given
            val dumpDir = Files.createTempDirectory("smart-http-dumps").toFile()
            var calls = 0
            val client = SmartHttpClient(
                client = HttpClient(
                    MockEngine {
                        calls++
                        respond(if (calls == 1) "body-v1" else "body-v2")
                    }
                ),
                responseDumpDir = dumpDir
            )

            try {
                // When: the zero ttl forces a revalidation on every call
                client.get<String>(
                    url = "https://example.test/data",
                    cacheKey = "test:dump:get",
                    ttl = 0.minutes
                )
                client.get<String>(
                    url = "https://example.test/data",
                    cacheKey = "test:dump:get",
                    ttl = 0.minutes
                )
                client.get<String>(
                    url = "https://example.test/data",
                    cacheKey = "test:dump:get",
                    ttl = 0.minutes
                )

                // Then: only the change was archived, with the replaced body kept aside
                // and every fetched body kept in the bounded history
                val keyDir = requireNotNull(dumpDir.listFiles()).single()
                assertEquals("body-v1", File(keyDir, "previous.body").readText())
                assertEquals("body-v2", File(keyDir, "latest.body").readText())
                assertEquals(2, File(keyDir, "history").listFiles()?.size)
            } finally {
                dumpDir.deleteRecursively()
            }
        }

        @Test
        @DisplayName("should expire a whole-minute ttl on a minute boundary")
        fun `should expire a whole-minute ttl on a minute boundary`() = runTest {
            // Given
            val client = SmartHttpClient(
                client = HttpClient(MockEngine { respond("v1") }),
                responseDumpDir = File("build/test-http-dumps")
            )

            // When
            client.get<String>(
                url = "https://example.test/data",
                cacheKey = "test:get:minute-ttl",
                ttl = 1.minutes
            )

            // Then: the entry expires on a wall-clock minute boundary, not a rolling delay
            val entry = requireNotNull(client.cache["test:get:minute-ttl"])
            assertEquals(0L, entry.expiresAtMillis % 60_000L)
            assertTrue(entry.expiresAtMillis > entry.fetchedAtMillis)
        }

        @Test
        @DisplayName("should keep a sub-minute ttl on a rolling delay")
        fun `should keep a sub-minute ttl on a rolling delay`() = runTest {
            // Given
            val client = SmartHttpClient(
                client = HttpClient(MockEngine { respond("v1") }),
                responseDumpDir = File("build/test-http-dumps")
            )

            // When
            client.get<String>(
                url = "https://example.test/data",
                cacheKey = "test:get:sub-minute-ttl",
                ttl = 30.seconds
            )

            // Then
            val entry = requireNotNull(client.cache["test:get:sub-minute-ttl"])
            assertEquals(
                entry.fetchedAtMillis + 30_000L,
                entry.expiresAtMillis
            )
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
                ),
                responseDumpDir = File("build/test-http-dumps")
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
                },
                responseDumpDir = File("build/test-http-dumps")
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

        @Test
        @DisplayName("should not archive the body of a changed POST response")
        fun `should not archive the body of a changed POST response`() = runTest {
            // Given
            val dumpDir = Files.createTempDirectory("smart-http-dumps").toFile()
            var calls = 0
            val client = SmartHttpClient(
                client = HttpClient(
                    MockEngine {
                        calls++
                        respond(if (calls == 1) "token-v1" else "token-v2")
                    }
                ),
                responseDumpDir = dumpDir
            )

            try {
                // When: the zero ttl forces a revalidation on every call
                client.post<String>(
                    url = "https://example.test/token",
                    body = "grant_type=client_id",
                    cacheKey = "test:dump:post",
                    ttl = 0.minutes
                )
                client.post<String>(
                    url = "https://example.test/token",
                    body = "grant_type=client_id",
                    cacheKey = "test:dump:post",
                    ttl = 0.minutes
                )

                // Then: POST bodies may carry credentials, nothing was written
                assertTrue(dumpDir.listFiles().isNullOrEmpty())
            } finally {
                dumpDir.deleteRecursively()
            }
        }
    }

    @Serializable
    private data class TokenRequestBody(val grantType: String)
}
