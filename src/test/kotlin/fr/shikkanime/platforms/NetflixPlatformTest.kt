package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeReleaseDayPlatformSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.ReleaseDayPlatformSimulcast
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.stream.Stream

class NetflixPlatformTest : AbstractTest() {
    @Inject private lateinit var netflixPlatform: NetflixPlatform

    data class NetflixTestCase(
        val netflixId: String,
        val expectedAnimeName: String,
        val releaseDay: Int,
        val testDate: String
    )

    companion object {
        @JvmStatic
        fun netflixTestCases(): Stream<NetflixTestCase> = Stream.of(
            NetflixTestCase(
                netflixId = "81497635",
                expectedAnimeName = "T・P BON",
                releaseDay = 3,
                testDate = "2024-07-17T07:00:00Z",
            ),
            NetflixTestCase(
                netflixId = "81562396",
                expectedAnimeName = "Four Knights of the Apocalypse",
                releaseDay = 7,
                testDate = "2024-09-22T00:00:00Z",
            ),
            NetflixTestCase(
                netflixId = "81943491",
                expectedAnimeName = "Dragon Ball DAIMA",
                releaseDay = 5,
                testDate = "2024-08-11T16:45:00Z",
            ),
            NetflixTestCase(
                netflixId = "81564905",
                expectedAnimeName = "My Happy Marriage",
                releaseDay = 1,
                testDate = "2025-03-24T14:00:00Z",
            ),
            NetflixTestCase(
                netflixId = "81208936",
                expectedAnimeName = "Violet Evergarden : Éternité et la poupée de souvenirs automatiques",
                releaseDay = 2,
                testDate = "2025-04-29T21:30:00Z",
            ),
            NetflixTestCase(
                netflixId = "81193214",
                expectedAnimeName = "Violet Evergarden : Le film",
                releaseDay = 2,
                testDate = "2025-04-29T21:30:00Z",
            ),
            NetflixTestCase(
                netflixId = "81050091",
                expectedAnimeName = "The Grimm Variations",
                releaseDay = 0,
                testDate = "2024-04-17T07:00:00Z"
            )
        )
    }

    @ParameterizedTest
    @MethodSource("netflixTestCases")
    fun `should fetch episodes from Netflix`(testCase: NetflixTestCase) {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse(testCase.testDate)
        val simulcastDay = ReleaseDayPlatformSimulcast().apply {
            name = testCase.netflixId
            releaseDay = testCase.releaseDay
        }
        
        val key = CountryCodeReleaseDayPlatformSimulcastKeyCache(countryCode, simulcastDay)
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
            assertTrue("https://" !in episode.getIdentifier())
        }
    }
}