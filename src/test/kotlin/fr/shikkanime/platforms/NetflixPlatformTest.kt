package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeNetflixSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.NetflixConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.stream.Stream

class NetflixPlatformTest : AbstractTest() {
    @Inject
    private lateinit var netflixPlatform: NetflixPlatform

    data class NetflixTestCase(
        val netflixId: String,
        val expectedAnimeName: String,
        val releaseDay: Int,
        val testDate: String,
        val imageUrl: String,
        val audioLocales: Set<String> = setOf("ja-JP"),
        val audioLocaleDelays: Map<String, Long> = emptyMap()
    )

    companion object {
        @JvmStatic
        fun netflixTestCases(): Stream<NetflixTestCase> = Stream.of(
            NetflixTestCase(
                netflixId = "81497635",
                expectedAnimeName = "T・P BON",
                releaseDay = 3,
                testDate = "2024-07-17T07:00:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1003/142645l.jpg"
            ),
            NetflixTestCase(
                netflixId = "81562396",
                expectedAnimeName = "Four Knights of the Apocalypse",
                releaseDay = 7,
                testDate = "2024-09-22T00:00:00Z",
                imageUrl = "https://www.manga-news.com/public/upload/2024/02/Four-Knights-of-the-Apocalypse-visual-3.jpg"
            ),
            NetflixTestCase(
                netflixId = "81943491",
                expectedAnimeName = "Dragon Ball DAIMA",
                releaseDay = 5,
                testDate = "2024-08-11T16:45:00Z",
                imageUrl = "https://imgsrv.crunchyroll.com/cdn-cgi/image/fit=contain,format=auto,quality=85,width=480,height=720/catalog/crunchyroll/298acc932735d9a731ea39a3db6a613c.jpg"
            ),
            NetflixTestCase(
                netflixId = "81564905",
                expectedAnimeName = "My Happy Marriage",
                releaseDay = 1,
                testDate = "2025-03-24T14:00:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1147/122444l.jpg",
                audioLocales = setOf("ja-JP", "fr-FR"),
                audioLocaleDelays = mapOf("fr-FR" to 1L)
            )
        )
    }

    @ParameterizedTest
    @MethodSource("netflixTestCases")
    fun `should fetch episodes from Netflix`(testCase: NetflixTestCase) {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse(testCase.testDate)
        val simulcastDay = NetflixConfiguration.NetflixSimulcastDay().apply {
            name = testCase.netflixId
            releaseDay = testCase.releaseDay
            image = testCase.imageUrl
            
            if (testCase.audioLocales.isNotEmpty()) {
                audioLocales = testCase.audioLocales.toMutableSet()
            }
            
            if (testCase.audioLocaleDelays.isNotEmpty()) {
                audioLocaleDelays = testCase.audioLocaleDelays.toMutableMap()
            }
        }
        
        val key = CountryCodeNetflixSimulcastKeyCache(countryCode, simulcastDay)
        val episodes = runBlocking { netflixPlatform.fetchApiContent(key, zonedDateTime) }
        
        assertNotNull(episodes)
        // Skip the test if no episodes are found
        // Maybe due to a region restriction
        assumeTrue(episodes.isNotEmpty())
        
        // Log episodes for debugging
        episodes.forEach {
            println(it)
        }
        
        // Common assertions for all episodes
        episodes.forEach { episode ->
            assertTrue(episode.url.isNotBlank())
            assertFalse(episode.getIdentifier().contains("https://"))
        }
    }
}