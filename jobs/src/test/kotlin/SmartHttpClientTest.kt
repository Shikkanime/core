package fr.shikkanime.jobs

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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
            val now = Clock.System.now().toEpochMilliseconds()
            val entry1 = SmartHttpClient.HttpCacheEntry("data1", 1, null, null, now + 10000)
            val entry2 = SmartHttpClient.HttpCacheEntry("data2", 2, null, null, now + 10000)
            val entry3 = SmartHttpClient.HttpCacheEntry("data3", 3, null, null, now + 10000)

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
            val now = Clock.System.now().toEpochMilliseconds()
            val expiredEntry = SmartHttpClient.HttpCacheEntry(
                data = "expired",
                contentHash = 123,
                etag = null,
                lastModified = null,
                expiresAtMillis = now - 200
            )

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
            val now = Clock.System.now().toEpochMilliseconds()
            val staleEntry = SmartHttpClient.HttpCacheEntry(
                data = "staleData",
                contentHash = 456,
                etag = "\"v1\"",
                lastModified = null,
                expiresAtMillis = now - 10.minutes.inWholeMilliseconds
            )

            // When
            client.cache["staleKey"] = staleEntry

            // Then
            assertEquals(staleEntry, client.cache["staleKey"])
            assertEquals(1, client.cache.size)
        }
    }
}
