package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeReleaseDayPlatformSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.ReleaseDayPlatformSimulcast
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class DisneyPlusPlatformTest : AbstractTest() {
    @Inject lateinit var disneyPlusPlatform: DisneyPlusPlatform

    @Test
    fun testGetApiContent() {
        disneyPlusPlatform.loadConfiguration()
        disneyPlusPlatform.configuration!!.apiCheckDelayInMinutes = 5
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0)
        val key = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, ReleaseDayPlatformSimulcast())
        val spyPlatform = spyk(disneyPlusPlatform)
        coEvery { spyPlatform.fetchApiContent(key, any()) } returns emptyList()

        // First fetch, OK
        runBlocking { spyPlatform.getApiContent(key, zonedDateTime) }
        assertEquals(zonedDateTime, spyPlatform.apiCache[key]!!.first)
        assertEquals(1, spyPlatform.apiCache.size)

        // Second fetch, OK
        runBlocking { spyPlatform.getApiContent(key, zonedDateTime.plusMinutes(5)) }
        assertEquals(zonedDateTime.plusMinutes(5), spyPlatform.apiCache[key]!!.first)
        assertEquals(1, spyPlatform.apiCache.size)

        // Third fetch, exception occurred during fetch
        coEvery { spyPlatform.fetchApiContent(key, any()) } throws Exception("Test exception")
        assertThrows<Exception> { runBlocking { spyPlatform.getApiContent(key, zonedDateTime.plusMinutes(10)) } }
        assertEquals(zonedDateTime.plusMinutes(5), spyPlatform.apiCache[key]!!.first)
        assertEquals(1, spyPlatform.apiCache.size)

        // Fourth fetch, OK
        coEvery { spyPlatform.fetchApiContent(key, any()) } returns emptyList()
        runBlocking { spyPlatform.getApiContent(key, zonedDateTime.plusMinutes(10).plusSeconds(20)) }
        assertEquals(zonedDateTime.plusMinutes(10), spyPlatform.apiCache[key]!!.first)
        assertEquals(1, spyPlatform.apiCache.size)

        // Fifth fetch, exception occurred during fetch
        coEvery { spyPlatform.fetchApiContent(key, any()) } throws Exception("Test exception")
        assertThrows<Exception> { runBlocking { spyPlatform.getApiContent(key, zonedDateTime.plusMinutes(15).plusSeconds(40)) } }
        assertEquals(zonedDateTime.plusMinutes(10), spyPlatform.apiCache[key]!!.first)
        assertEquals(1, spyPlatform.apiCache.size)

        // Sixth fetch, OK
        coEvery { spyPlatform.fetchApiContent(key, any()) } returns emptyList()
        runBlocking { spyPlatform.getApiContent(key, zonedDateTime.plusMinutes(16)) }
        assertEquals(zonedDateTime.plusMinutes(15), spyPlatform.apiCache[key]!!.first)
        assertEquals(1, spyPlatform.apiCache.size)
    }
}