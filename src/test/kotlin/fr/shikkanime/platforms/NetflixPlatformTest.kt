package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class NetflixPlatformTest : AbstractTest() {
    @Inject
    private lateinit var netflixPlatform: NetflixPlatform

    @BeforeEach
    override fun setUp() {
        super.setUp()
        AbstractNetflixWrapper.checkLanguage = false
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        AbstractNetflixWrapper.checkLanguage = true
    }

    @Test
    fun fetchApiContent() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-07-17T07:00:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay(
            3,
            "https://cdn.myanimelist.net/images/anime/1003/142645l.jpg",
            "07:00:00"
        ).apply {
            name = "81497635"
        })
        val episodes = runCatching { runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) } }.getOrNull() ?: emptyList()
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
    fun `fetchApiContentFor The Seven Deadly Sins Four Knights of the Apocalypse`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-09-22T00:00:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay(
            7,
            "https://www.manga-news.com/public/upload/2024/02/Four-Knights-of-the-Apocalypse-visual-3.jpg",
            "00:00:00"
        ).apply {
            name = "81562396"
        })
        val episodes = runCatching { runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) } }.getOrNull() ?: emptyList()
        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())
        assertEquals("Alors qu'une prophétie funeste plane sur le paisible royaume de Britannia, un garçon au cœur pur se lance dans un périple captivant de découverte… et de vengeance.", episodes.first().animeDescription)

        episodes.forEach {
            println(it)
        }

        assertEquals(36, episodes.size)
        // Count the distinct episode identifiers
        assertEquals(36, episodes.map { it.getIdentifier() }.distinct().size)
        assertEquals("FR-NETF-8a6184b0-JA-JP", episodes.first().getIdentifier())
    }

    @Test
    fun `fetchApiContent for Dragon Ball DAIMA`() {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse("2024-08-11T16:45:00Z")
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, NetflixConfiguration.NetflixSimulcastDay(
            5,
            "https://imgsrv.crunchyroll.com/cdn-cgi/image/fit=contain,format=auto,quality=85,width=480,height=720/catalog/crunchyroll/298acc932735d9a731ea39a3db6a613c.jpg",
            "16:45:00"
        ).apply {
            name = "81943491"
        })
        val episodes = runCatching { runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) } }.getOrNull() ?: emptyList()
        assertNotNull(episodes)
        assumeTrue(episodes.isNotEmpty())

        episodes.forEach {
            println(it)
        }

        assertTrue(episodes.size >= 4)
    }
}