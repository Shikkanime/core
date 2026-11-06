package fr.shikkanime.jobs

import fr.shikkanime.cache.Cache
import fr.shikkanime.cache.L1Cache
import fr.shikkanime.cache.ValkeyWrapper
import fr.shikkanime.database.cache.CacheBuckets
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

@DisplayName("tests for the catalog cache invalidator")
class CatalogCacheInvalidatorTest {
    /**
     * [ValkeyWrapper] recording the version keys a bucket invalidation increments.
     */
    private class RecordingValkeyWrapper : ValkeyWrapper {
        val incrementedKeys = mutableListOf<String>()

        override suspend fun get(key: String): ByteArray? =
            null

        override suspend fun set(key: String, value: ByteArray, ttl: Duration) =
            Unit

        override suspend fun incr(key: String): Long {
            incrementedKeys += key
            return 1L
        }
    }

    @Test
    @DisplayName("should increment the version of the anime and episode buckets")
    fun `should increment the version of the anime and episode buckets`() =
        runBlocking {
            // Given
            val valkeyWrapper = RecordingValkeyWrapper()
            val invalidator = CatalogCacheInvalidator(
                Cache(l1 = L1Cache(), valkeyWrapper = valkeyWrapper)
            )

            // When
            invalidator.invalidate()

            // Then
            assertEquals(
                listOf("ver:${CacheBuckets.ANIME}", "ver:${CacheBuckets.EPISODE}"),
                valkeyWrapper.incrementedKeys
            )
        }
}
