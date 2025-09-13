package fr.shikkanime

import fr.shikkanime.utils.AsynchronizedGlideClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class ValkeyMassKeysTest {
    private val prefix = "testValkeyKey"
    private val random = SecureRandom()

    // Use system property to allow overriding the number of keys. Default to a large value.
    private val keysCount: Int = runCatching {
        System.getProperty("valkey.mass.count")?.toInt()
    }.getOrNull() ?: 100_000

    // Limit how many keys we'll sample for read verification
    private val sampleCount: Int = runCatching {
        System.getProperty("valkey.mass.sample")?.toInt()
    }.getOrNull() ?: 200

    @BeforeEach
    fun checkAvailabilityAndCleanBefore() {
        // Skip if Valkey is not available in the environment
        assumeTrue(AsynchronizedGlideClient.isAvailable()) {
            "Valkey not available: skipping mass-keys test."
        }

        // Clean any leftover keys from previous runs
        val before = findAllPrefixedKeys()
        if (before.isNotEmpty()) {
            AsynchronizedGlideClient.delInPool(before)
            AsynchronizedGlideClient.del()
        }
        val after = findAllPrefixedKeys()
        assertEquals(0, after.size, "Expected no remaining keys before starting the test")
    }

    @AfterEach
    fun cleanAfter() {
        val keys = findAllPrefixedKeys()
        if (keys.isNotEmpty()) {
            AsynchronizedGlideClient.delInPool(keys)
            AsynchronizedGlideClient.del()
        }
    }

    @Test
    fun massInsertionReadAndCleanup_shouldBehaveCorrectly() {
        // 1) Generate keys + values and write them
        val created = mutableListOf<String>()
        repeat(keysCount) { idx ->
            val k = "$prefix:${randomSuffix(64)}"
            val v = "test:$idx"
            AsynchronizedGlideClient[k] = v
            created.add(k)
        }

        // 2) Verify count using scan
        val allKeys = findAllPrefixedKeys()
        assertEquals(keysCount, allKeys.size, "All inserted keys should be present")

        // 3) Verify a sample of reads
        val sampledKeys = created.shuffled(random).take(sampleCount)
        sampledKeys.forEach { k ->
            val rawIndex = created.indexOf(k)
            val expected = "test:$rawIndex"
            val value = AsynchronizedGlideClient[k]
            assertNotNull(value, "Value should not be null for key $k")
            assertEquals(expected, value, "Value mismatch for key $k")
        }

        // 4) Delete and verify cleanup
        AsynchronizedGlideClient.delInPool(allKeys)
        AsynchronizedGlideClient.del()

        val remaining = findAllPrefixedKeys()
        assertEquals(0, remaining.size, "All keys should have been deleted")
    }

    private fun findAllPrefixedKeys(): List<String> =
        AsynchronizedGlideClient.searchAll("$prefix*", count = 5_000).filter {
            val substringAfter = it.substringAfter(prefix)
            substringAfter.isBlank() || substringAfter.startsWith(":")
        }

    private fun randomSuffix(length: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789(),;"
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        val sb = StringBuilder(length)
        repeat(length) { i ->
            val idx = (bytes[i].toInt() and 0xFF) % alphabet.length
            sb.append(alphabet[idx])
        }
        return sb.toString()
    }
}
