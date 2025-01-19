package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodePrimeVideoSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class PrimeVideoPlatformTest : AbstractTest() {
    @Inject
    private lateinit var primeVideoPlatform: PrimeVideoPlatform

    @Test
    fun fetchApiContent() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-04-22T18:15:00Z")
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast(
            1,
            "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
        ).apply {
            name = "0QN9ZXJ935YBTNK8U9FV5OAX5B"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) }

        assertNotNull(episodes)
        assumeTrue(episodes!!.isNotEmpty())
        assertEquals(13, episodes.size)

        episodes.forEach {
            assertEquals("Ninja Kamui", it.anime)
            assertTrue(it.animeDescription?.isNotBlank() == true)
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }
    }

    @Test
    fun `fetchApiContent #2`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2025-01-10T16:31:00Z")
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast(
            1,
            "https://cdn.myanimelist.net/images/anime/1850/144045l.jpg",
        ).apply {
            name = "0TRGLGKLJS99OYM9647IM6Y1N0"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) }

        assertNotNull(episodes)
        assumeTrue(episodes!!.isNotEmpty())

        episodes.forEach {
            assertEquals("Übel Blatt", it.anime)
            assertTrue(it.animeDescription?.isNotBlank() == true)
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }
    }

    @Test
    fun `fetchApiContent #3`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-04-22T18:15:00Z")
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast(
            1,
            "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
        ).apply {
            name = "0QA3P8T387P0WAV0KXUYBWDDYR"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) }

        assertNotNull(episodes)
        assumeTrue(episodes!!.isNotEmpty())

        episodes.forEach {
            assertEquals("Magilumière Co. Ltd.", it.anime)
            assertTrue(it.animeDescription?.isNotBlank() == true)
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }
    }

    @Test
    fun `fix bug on getApiContent`() {
        val zonedDateTime = ZonedDateTime.parse("2024-04-22T18:15:00Z")

        val primeVideoSimulcast = PrimeVideoConfiguration.PrimeVideoSimulcast(
            zonedDateTime.dayOfWeek.value,
            "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
        ).apply {
            name = "0QA3P8T387P0WAV0KXUYBWDDYR"
        }

        primeVideoPlatform.configuration!!.availableCountries = setOf(CountryCode.FR)
        primeVideoPlatform.configuration!!.addPlatformSimulcast(primeVideoSimulcast)
        primeVideoPlatform.configuration!!.apiCheckDelayInMinutes = 5
        primeVideoPlatform.saveConfiguration()

        runBlocking { primeVideoPlatform.fetchEpisodes(zonedDateTime, null) }
        primeVideoPlatform.updateAnimeSimulcastConfiguration(primeVideoSimulcast.name)
        assertEquals(1, primeVideoPlatform.apiCache.size)

        runBlocking { primeVideoPlatform.fetchEpisodes(zonedDateTime.plusMinutes(5), null) }
        primeVideoPlatform.updateAnimeSimulcastConfiguration("test")
        assertEquals(1, primeVideoPlatform.apiCache.size)

        runBlocking { primeVideoPlatform.fetchEpisodes(zonedDateTime.plusMinutes(5).plusSeconds(20), null) }
        assertEquals(1, primeVideoPlatform.apiCache.size)
    }
}