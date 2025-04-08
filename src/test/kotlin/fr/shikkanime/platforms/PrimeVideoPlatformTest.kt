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
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
            name = "0QN9ZXJ935YBTNK8U9FV5OAX5B"
            releaseDay = 1
            image = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) } ?: emptyList()

        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())
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
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
            name = "0TRGLGKLJS99OYM9647IM6Y1N0"
            releaseDay = 1
            image = "https://cdn.myanimelist.net/images/anime/1850/144045l.jpg"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) } ?: emptyList()

        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

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
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
            name = "0QA3P8T387P0WAV0KXUYBWDDYR"
            releaseDay = 1
            image = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) } ?: emptyList()

        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

        episodes.forEach {
            assertEquals("Magilumière Co. Ltd.", it.anime)
            assertTrue(it.animeDescription?.isNotBlank() == true)
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }
    }

    @Test
    fun `fetchApiContent #4`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2025-04-08T16:01:00Z")
        val key = CountryCodePrimeVideoSimulcastKeyCache(countryCode, PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
            name = "0KFD79CBVX90IWRIG5NLAGJ7R1"
            releaseDay = 2
            image = "https://cdn.myanimelist.net/images/anime/1803/146807l.jpg"
        })
        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) } ?: emptyList()

        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

        episodes.forEach {
            assertEquals("Mobile Suit Gundam GQuuuuuuX", it.anime)
            assertTrue(it.animeDescription?.isNotBlank() == true)
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }
    }

    @Test
    fun `fix bug on getApiContent`() {
        val zonedDateTime = ZonedDateTime.parse("2024-04-22T18:15:00Z")

        val primeVideoSimulcast = PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
            name = "0QA3P8T387P0WAV0KXUYBWDDYR"
            releaseDay = zonedDateTime.dayOfWeek.value
            image = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg"
        }

        primeVideoPlatform.configuration!!.availableCountries = setOf(CountryCode.FR)
        primeVideoPlatform.configuration!!.addPlatformSimulcast(primeVideoSimulcast)
        primeVideoPlatform.configuration!!.apiCheckDelayInMinutes = 5
        primeVideoPlatform.saveConfiguration()

        runCatching { primeVideoPlatform.fetchEpisodes(zonedDateTime, null) }
        primeVideoPlatform.updateAnimeSimulcastConfiguration(primeVideoSimulcast.name)
        assumeTrue(1 == primeVideoPlatform.apiCache.size)

        runCatching { primeVideoPlatform.fetchEpisodes(zonedDateTime.plusMinutes(5), null) }
        primeVideoPlatform.updateAnimeSimulcastConfiguration("test")
        assumeTrue(1 == primeVideoPlatform.apiCache.size)

        runCatching { primeVideoPlatform.fetchEpisodes(zonedDateTime.plusMinutes(5).plusSeconds(20), null) }
        assumeTrue(1 == primeVideoPlatform.apiCache.size)
    }
}