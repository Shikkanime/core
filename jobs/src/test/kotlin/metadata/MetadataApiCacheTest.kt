package fr.shikkanime.jobs.metadata

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.services.ApiCacheService
import fr.shikkanime.models.ApiCacheEntry
import fr.shikkanime.models.MetadataPlatform
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class MetadataApiCacheTest {
    private val apiCacheService = mockk<ApiCacheService>()
    private val cache = MetadataApiCache(apiCacheService)

    @Test
    fun `returns the cached body while the entry is fresh`() {
        // Given
        val entry = ApiCacheEntry(
            id = null, createdAt = TimeUtils.now(), updatedAt = null,
            key = "k", body = "cached-body"
        )
        every { apiCacheService.findByKey("k") } returns entry

        // When
        val body = runBlocking { cache.getOrPut("k", 7.days) { "fetched" } }

        // Then
        assertEquals("cached-body", body)
        verify(exactly = 0) { apiCacheService.save(any()) }
    }

    @Test
    fun `fetches and stores the body when the entry is stale`() {
        // Given
        val stale = ApiCacheEntry(
            id = null,
            createdAt = LocalDateTime(2000, 1, 1, 0, 0),
            updatedAt = null,
            key = "k", body = "old-body"
        )
        every { apiCacheService.findByKey("k") } returns stale
        every { apiCacheService.save(any()) } answers { firstArg() }

        // When
        val body = runBlocking { cache.getOrPut("k", 7.days) { "fetched-body" } }

        // Then
        assertEquals("fetched-body", body)
        verify { apiCacheService.save(match { it.key == "k" && it.body == "fetched-body" }) }
    }

    @Test
    fun `fetches and stores the body on cache miss`() {
        // Given
        every { apiCacheService.findByKey("k") } returns null
        every { apiCacheService.save(any()) } answers { firstArg() }

        // When
        val body = runBlocking { cache.getOrPut("k", 1.hours) { "fetched-body" } }

        // Then
        assertEquals("fetched-body", body)
        verify { apiCacheService.save(any()) }
    }

    @Test
    fun `builds a bounded cache key`() {
        // When
        val key = cache.key(
            MetadataPlatform.ANILIST, "search",
            "a very long title ".repeat(40)
        )

        // Then
        assertTrue(key.length <= 512)
        assertTrue(key.startsWith("anilist:search:"))
    }
}
