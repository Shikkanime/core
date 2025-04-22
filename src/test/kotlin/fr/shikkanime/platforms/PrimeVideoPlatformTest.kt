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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.stream.Stream

class PrimeVideoPlatformTest : AbstractTest() {
    @Inject
    private lateinit var primeVideoPlatform: PrimeVideoPlatform

    data class SimulcastTestCase(
        val simulcastName: String,
        val expectedAnimeName: String,
        val releaseDay: Int,
        val testDate: String,
        val imageUrl: String,
        val expectedAudioLocales: Set<String>
    )

    companion object {
        @JvmStatic
        fun primeVideoTestCases(): Stream<SimulcastTestCase> = Stream.of(
            SimulcastTestCase(
                simulcastName = "0QN9ZXJ935YBTNK8U9FV5OAX5B",
                expectedAnimeName = "Ninja Kamui",
                releaseDay = 1,
                testDate = "2024-04-22T18:15:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
                expectedAudioLocales = setOf("ja-JP")
            ),
            SimulcastTestCase(
                simulcastName = "0TRGLGKLJS99OYM9647IM6Y1N0",
                expectedAnimeName = "Übel Blatt",
                releaseDay = 1,
                testDate = "2025-01-10T16:31:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1850/144045l.jpg",
                expectedAudioLocales = setOf("ja-JP", "fr-FR")
            ),
            SimulcastTestCase(
                simulcastName = "0QA3P8T387P0WAV0KXUYBWDDYR",
                expectedAnimeName = "Magilumière Co. Ltd.",
                releaseDay = 1,
                testDate = "2024-04-22T18:15:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
                expectedAudioLocales = setOf("ja-JP", "fr-FR")
            ),
            SimulcastTestCase(
                simulcastName = "0TKRFV2FB8U18G5WS16GFOXNG2",
                expectedAnimeName = "The Dinner Table Detective",
                releaseDay = 6,
                testDate = "2025-04-05T03:01:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1496/146890l.jpg",
                expectedAudioLocales = setOf("ja-JP", "fr-FR")
            ),
            SimulcastTestCase(
                simulcastName = "0GMSR5WMNJ6EMYQRQ6Y2587WAF",
                expectedAnimeName = "From Old Country Bumpkin to Master Swordsman",
                releaseDay = 6,
                testDate = "2025-04-05T15:01:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1069/148148l.jpg",
                expectedAudioLocales = setOf("ja-JP", "fr-FR")
            ),
            SimulcastTestCase(
                simulcastName = "0KFD79CBVX90IWRIG5NLAGJ7R1",
                expectedAnimeName = "Mobile Suit Gundam GQuuuuuuX",
                releaseDay = 2,
                testDate = "2025-04-08T16:01:00Z",
                imageUrl = "https://cdn.myanimelist.net/images/anime/1803/146807l.jpg",
                expectedAudioLocales = setOf("ja-JP", "fr-FR")
            ),
            SimulcastTestCase(
                simulcastName = "0NV4FUWKV9BQN8VDIIH1DWEEHH",
                expectedAnimeName = "Lazarus",
                releaseDay = 6,
                testDate = "2025-04-05T00:01:00Z",
                imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx167336-KpGIIBie71OX.png",
                expectedAudioLocales = setOf("en-US", "fr-FR")
            )
        )
    }

    @ParameterizedTest
    @MethodSource("primeVideoTestCases")
    fun `should fetch episodes from Prime Video`(testCase: SimulcastTestCase) {
        val countryCode = CountryCode.FR
        val zonedDateTime = ZonedDateTime.parse(testCase.testDate)
        val key = CountryCodePrimeVideoSimulcastKeyCache(
            countryCode,
            PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
                name = testCase.simulcastName
                releaseDay = testCase.releaseDay
                image = testCase.imageUrl
            }
        )

        val episodes = runBlocking { primeVideoPlatform.fetchApiContent(key, zonedDateTime) }

        assertNotNull(episodes)
        // Skip the test if no episodes are found
        // Maybe due to a region restriction
        assumeTrue(episodes.isNotEmpty())

        // Log episodes for debugging
        episodes.forEach {
            println(it)
        }

        // Find if duplicate episodes exist
        val duplicates = episodes.groupBy { it.getIdentifier() }
            .filter { it.value.size > 1 }
            .map { it.key }
            .toList()
        assertTrue(duplicates.isEmpty(), "Duplicate episodes found: $duplicates")

        // Common assertions for all episodes
        episodes.forEach {
            assertEquals(testCase.expectedAnimeName, it.anime)
            assertTrue(it.animeDescription?.isNotBlank() == true)
            assertTrue(it.image.startsWith("https://m.media-amazon.com"))
            assertTrue(it.url.isNotBlank())
            assertFalse(it.getIdentifier().contains("https://"))
        }

        // Check if the audio locales match the expected values
        val audioLocales = episodes.map { it.audioLocale }.toSet()
        assertTrue(audioLocales.containsAll(testCase.expectedAudioLocales), "Audio locales do not match")
    }

    @Test
    fun `should use cache within API check delay period`() {
        // Setup test configuration
        val testCase = primeVideoTestCases().findFirst().get()
        val zonedDateTime = ZonedDateTime.parse(testCase.testDate)
        
        val simulcast = PrimeVideoConfiguration.PrimeVideoSimulcast().apply {
            name = testCase.simulcastName
            releaseDay = zonedDateTime.dayOfWeek.value
            image = testCase.imageUrl
        }

        // Configure platform
        primeVideoPlatform.configuration!!.apply {
            availableCountries = setOf(CountryCode.FR)
            addPlatformSimulcast(simulcast)
            apiCheckDelayInMinutes = 5
        }
        primeVideoPlatform.saveConfiguration()

        // First fetch - should populate cache
        runCatching { primeVideoPlatform.fetchEpisodes(zonedDateTime, null) }
        primeVideoPlatform.updateAnimeSimulcastConfiguration(simulcast.name)
        assertEquals(1, primeVideoPlatform.apiCache.size)

        // Second fetch within delay - should use cache 
        runCatching { primeVideoPlatform.fetchEpisodes(zonedDateTime.plusMinutes(3), null) }
        primeVideoPlatform.updateAnimeSimulcastConfiguration("test")
        assertEquals(1, primeVideoPlatform.apiCache.size)

        // Third fetch still within delay - should use cache
        runCatching { primeVideoPlatform.fetchEpisodes(zonedDateTime.plusMinutes(4).plusSeconds(30), null) }
        assertEquals(1, primeVideoPlatform.apiCache.size)
    }
}