package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class NetflixPlatformTest : AbstractTest() {
    @Inject
    private lateinit var netflixPlatform: NetflixPlatform

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
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assumeTrue(episodes != null)
        assumeTrue(episodes!!.isNotEmpty())

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
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assumeTrue(episodes != null)
        assumeTrue(episodes!!.isNotEmpty())
        assumeTrue("Alors qu'une prophétie funeste plane sur le paisible royaume de Britannia, un garçon au cœur pur se lance dans un périple captivant de découverte… et de vengeance." == episodes.first().animeDescription)

        episodes.forEach {
            println(it)
        }

        assertEquals(24, episodes.size)
        // Count the distinct episode identifiers
        assertEquals(24, episodes.map { it.getIdentifier() }.distinct().size)
        assertTrue(episodes.sumOf { it.duration } > 0)
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
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        assumeTrue(episodes != null)
        assumeTrue(episodes!!.isNotEmpty())

        episodes.forEach {
            println(it)
        }

        assertTrue(episodes.size >= 4)
    }
}