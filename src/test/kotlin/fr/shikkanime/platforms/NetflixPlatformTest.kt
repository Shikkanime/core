package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class NetflixPlatformTest : AbstractTest() {
    @Inject
    private lateinit var netflixPlatform: NetflixPlatform

    @Test
    fun fetchApiContent() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-07-17T07:00:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay().apply {
            name = "81497635"
            releaseDay = 3
            image = "https://cdn.myanimelist.net/images/anime/1003/142645l.jpg"
        })
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

        episodes.forEach {
            println(it)
        }

        assertEquals(24, episodes.size)
        // Count the distinct episode identifiers
        assertEquals(24, episodes.map { it.getIdentifier() }.distinct().size)
    }

    @Test
    fun `fetchApiContent For The Seven Deadly Sins Four Knights of the Apocalypse`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-09-22T00:00:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay().apply {
            name = "81562396"
            releaseDay = 7
            image = "https://www.manga-news.com/public/upload/2024/02/Four-Knights-of-the-Apocalypse-visual-3.jpg"
        })
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())
        assertEquals("Famine, Guerre, Mort, Épidémie. Alors qu'une prophétie apocalyptique plane sur Britannia, une nouvelle génération de héros doit unir ses forces pour sauver le monde.", episodes.first().animeDescription)

        episodes.forEach {
            println(it)
        }

        assertEquals(36, episodes.size)
        // Count the distinct episode identifiers
        assertEquals(36, episodes.map { it.getIdentifier() }.distinct().size)
        assertEquals("FR-NETF-81700064-JA-JP", episodes.first().getIdentifier())
    }

    @Test
    fun `fetchApiContent for Dragon Ball DAIMA`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-08-11T16:45:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay().apply {
            name = "81943491"
            releaseDay = 5
            image = "https://imgsrv.crunchyroll.com/cdn-cgi/image/fit=contain,format=auto,quality=85,width=480,height=720/catalog/crunchyroll/298acc932735d9a731ea39a3db6a613c.jpg"
        })
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

        episodes.forEach {
            println(it)
        }

        assertTrue(episodes.size >= 4)
    }

    @Test
    fun `fetchApiContent for My Happy Mariage`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2025-03-24T14:00:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay().apply {
            name = "81564905"
            releaseDay = 1
            image = "https://cdn.myanimelist.net/images/anime/1147/122444l.jpg"
            audioLocales = mutableSetOf("ja-JP", "fr-FR")
            audioLocaleDelays = mutableMapOf("fr-FR" to 1)
        })
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

        episodes.forEach {
            println(it)
        }

        val firstEpisode = episodes.filter { it.id == "81651189" }
        assertEquals(2, firstEpisode.size)
        assertEquals("FR-NETF-81651189-JA-JP", firstEpisode.first().getIdentifier())
        assertEquals("FR-NETF-81651189-FR-FR", firstEpisode.last().getIdentifier())
        // Check the delay for the French audio locale
        assertEquals(1, ChronoUnit.WEEKS.between(firstEpisode.first().releaseDateTime, firstEpisode.last().releaseDateTime))
    }
}