package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.SmartHttpClient
import io.mockk.every
import io.mockk.mockk

/**
 * MockK [TestBuilder] producing [SmartHttpClient.HttpCacheEntry] test fixtures with
 * conditionally stubbed fields.
 *
 * Only the fields set by the test are stubbed; the cache logic reads them back through
 * the mock getters.
 *
 * @param configuration Lambda receiving the builder to set the required fixture fields.
 * @see [SmartHttpClient.HttpCacheEntry]
 * @see [TestBuilder]
 */
class HttpCacheEntryMockKBuilder(
    configuration: HttpCacheEntryMockKBuilder.() -> Unit = {}
) : TestBuilder<SmartHttpClient.HttpCacheEntry> {
    var data: Any? = null
    var contentHash: Int? = null
    var etag: String? = null
    var lastModified: String? = null
    var expiresAtMillis: Long? = null

    init {
        configuration(this)
    }

    override fun build(): SmartHttpClient.HttpCacheEntry {
        val mockK = mockk<SmartHttpClient.HttpCacheEntry>(relaxed = true)
        data?.let { every { mockK.data } returns it }
        contentHash?.let { every { mockK.contentHash } returns it }
        etag?.let { every { mockK.etag } returns it }
        lastModified?.let { every { mockK.lastModified } returns it }
        expiresAtMillis?.let { every { mockK.expiresAtMillis } returns it }

        return mockK
    }
}
