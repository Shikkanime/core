package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PlatformSimulcast
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.time.ZonedDateTime
import java.util.stream.Stream

class CrunchyrollPlatformTest : AbstractTest() {
    @Inject lateinit var platform: CrunchyrollPlatform

    @BeforeEach
    override fun setUp() {
        super.setUp()
        platform.loadConfiguration()
        platform.configuration!!.availableCountries = setOf(CountryCode.FR)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        platform.configuration!!.simulcasts.clear()
    }
    
    data class EpisodeTestCase(
        val testDate: String,
        val simulcastNames: List<String>,
        val expectedAnimeName: String,
        val episodeType: EpisodeType? = null,
        val expectedEpisodeCount: Int? = null,
        val expectedSeason: Int? = null,
        val expectedNumber: Int? = null,
        val needsEpisodeVariantSetup: Boolean = false
    )
    
    companion object {
        @JvmStatic
        fun crunchyrollTestCases(): Stream<EpisodeTestCase> = Stream.of(
            EpisodeTestCase(
                testDate = "2024-01-24T18:45:00Z",
                simulcastNames = listOf("metallic rouge"),
                expectedAnimeName = "Metallic Rouge",
                expectedEpisodeCount = 2
            ),
            EpisodeTestCase(
                testDate = "2024-04-14T09:00:00Z",
                simulcastNames = listOf("one piece"),
                expectedAnimeName = "One Piece",
                episodeType = EpisodeType.SPECIAL,
                expectedNumber = 13
            ),
            EpisodeTestCase(
                testDate = "2024-06-08T12:45:00Z",
                simulcastNames = listOf("kaiju no. 8"),
                expectedAnimeName = "Kaiju No. 8",
                episodeType = EpisodeType.EPISODE,
                expectedNumber = 9
            ),
            EpisodeTestCase(
                testDate = "2024-07-08T07:30:00Z",
                simulcastNames = listOf("days with my stepsister", "mayonaka punch"),
                expectedAnimeName = "Days with My Stepsister",
                expectedEpisodeCount = 1,
                episodeType = EpisodeType.EPISODE,
                expectedSeason = 1,
                expectedNumber = 1
            ),
            EpisodeTestCase(
                testDate = "2024-07-17T15:00:00Z",
                simulcastNames = listOf("alya sometimes hides her feelings in russian"),
                expectedAnimeName = "Alya Sometimes Hides Her Feelings in Russian",
                expectedEpisodeCount = 1,
                episodeType = EpisodeType.EPISODE,
                expectedSeason = 1,
                expectedNumber = 3,
                needsEpisodeVariantSetup = true
            ),
            EpisodeTestCase(
                testDate = "2024-10-24T22:00:00Z",
                simulcastNames = listOf(),
                expectedAnimeName = "BOCCHI THE ROCK!",
                expectedEpisodeCount = 12
            ),
            EpisodeTestCase(
                testDate = "2024-10-25T18:15:00Z",
                simulcastNames = listOf(),
                expectedAnimeName = "Gridman Universe",
                expectedEpisodeCount = 1,
                episodeType = EpisodeType.FILM
            )
        )
        
        @JvmStatic
        fun negativeTestCases(): Stream<String> = Stream.of(
            "2025-03-28T06:30:00Z"
        )
    }

    @ParameterizedTest
    @MethodSource("crunchyrollTestCases")
    fun `should fetch episodes from JSON files`(testCase: EpisodeTestCase) {
        // Setup simulcasts
        testCase.simulcastNames.forEach { simulcastName ->
            platform.configuration!!.simulcasts.add(PlatformSimulcast(name = simulcastName))
        }
        
        // Setup episode variant if needed
        if (testCase.needsEpisodeVariantSetup) {
            setupEpisodeVariantForAlya()
        }
        
        val zonedDateTime = ZonedDateTime.parse(testCase.testDate)
        val formattedDate = testCase.testDate.replace(':', '-')
        
        // Load episodes from test JSON file
        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-$formattedDate.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != testCase.expectedAnimeName }
        
        // Verify common expectations
        assertTrue(episodes.isNotEmpty())
        
        // Verify specific expectations if provided
        if (testCase.expectedEpisodeCount != null) {
            assertEquals(testCase.expectedEpisodeCount, episodes.size)
        }
        
        // Verify the first episode properties
        episodes.firstOrNull()?.let { firstEpisode ->
            assertEquals(testCase.expectedAnimeName, firstEpisode.anime)
            
            testCase.episodeType?.let { expectedType ->
                assertEquals(expectedType, firstEpisode.episodeType)
            }
            
            testCase.expectedSeason?.let { expectedSeason ->
                assertEquals(expectedSeason, firstEpisode.season)
            }
            
            testCase.expectedNumber?.let { expectedNumber ->
                assertEquals(expectedNumber, firstEpisode.number)
            }
        }
    }
    
    private fun setupEpisodeVariantForAlya() {
        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "G1XHJV0XM",
                "Alya Sometimes Hides Her Feelings in Russian",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/b0bdcf73a7e00f9bc75131088970288d.jpg",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/2db2c99a90bc322a2fe8a2fa07810fd5.jpg",
                "Dans sa nouvelle école, Alya, étudiante au comportement glacial, fait tourner les têtes. Malgré ses excellents résultats en classe, elle garde ses distances avec tout le monde, en particulier avec l'intello Masachika Kuze. Jusqu'à ce qu'elle lui fasse un compliment en... russe ! À l'insu d'Alya, Kuze la comprend parfaitement, mais joue le jeu. Leurs malentendus hilarants se transformeront-ils en amour ?",
                ZonedDateTime.parse("2024-07-10T15:00:00Z"),
                EpisodeType.EPISODE,
                "G6JQC1ZXV",
                1,
                2,
                1479,
                "Amis d'enfance ?",
                "Alors que les élections approchent, Masachika ne montre aucun intérêt pour celles-ci, malgré les suggestions de ses camarades. Il décidé alors d'accompagner Yuki, qui est en réalité sa sœur, lors d'une sortie.",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/6deef00e3ccbf856cdddb2b75a614fc2.jpg",
                Platform.CRUN,
                "ja-JP",
                "G9DUE0QNJ",
                "https://www.crunchyroll.com/fr/watch/G9DUE0QNJ/so-much-for-childhood-friends",
                uncensored = false,
                original = true,
            ),
            updateMappingDateTime = false
        )
        MapCache.invalidate(EpisodeVariant::class.java)
    }
    
    @ParameterizedTest
    @MethodSource("negativeTestCases")
    fun `should not find specific anime in test dates`(testDate: String) {
        val zonedDateTime = ZonedDateTime.parse(testDate)
        val formattedDate = testDate.replace(':', '-')
        
        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-$formattedDate.json")?.file
                    ?: throw Exception("File not found")
            )
        )
        
        // Verify specific exclusions for March 28, 2025
        assertTrue(episodes.none { it.anime == "Teogonia" })
        assertTrue(episodes.none { it.anime == "Can a Boy-Girl Friendship Survive?" })
        assertTrue(episodes.none { it.anime == "The Brilliant Healer's New Life in the Shadows" })
    }

    @Test
    fun fetchNextEpisodeSuccessfully() {
        val expectedEpisode = mockkClass(AbstractCrunchyrollWrapper.BrowseObject::class)

        mockkStatic(CrunchyrollWrapper::class) {
            coEvery { CrunchyrollWrapper.getUpNext(any(String::class), any(String::class)) } returns expectedEpisode
            val result = runBlocking { platform.getNextEpisode(CountryCode.FR, "someId") }
            assertEquals(expectedEpisode, result)
        }
    }

    @Test
    fun getNextEpisodeFallbackToEpisode() = runBlocking {
        val episode = AbstractCrunchyrollWrapper.Episode(
            "",
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            emptyList(),
            ZonedDateTime.now(),
            StringUtils.EMPTY_STRING,
            null,
            null,
            StringUtils.EMPTY_STRING,
            null,
            null,
            null,
            null,
            1440000L,
            null,
            false,
            null,
            "nextId",
            1,
            1.0,
            null
        )
        val expectedEpisode = mockkClass(AbstractCrunchyrollWrapper.BrowseObject::class)

        mockkStatic(CrunchyrollWrapper::class) {
            coEvery { CrunchyrollWrapper.getUpNext(any(String::class), any(String::class)) } throws Exception()
            coEvery { CrunchyrollWrapper.getJvmStaticEpisode(any(String::class), any(String::class)) } returns episode
            coEvery { CrunchyrollWrapper.getJvmStaticObjects(any(String::class), any(String::class)) } returns listOf(expectedEpisode)
            val result = runBlocking { platform.getNextEpisode(CountryCode.FR, "someId") }
            assertEquals(expectedEpisode, result)
        }
    }

    @Test
    fun getNextEpisodeFallbackToSeason() = runBlocking {
        val countryCode = CountryCode.FR
        val crunchyrollId = "someId"
        val episode = AbstractCrunchyrollWrapper.Episode(
            "",
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            emptyList(),
            ZonedDateTime.now().minusDays(1),
            "seasonId",
            null,
            null,
            StringUtils.EMPTY_STRING,
            null,
            null,
            null,
            null,
            1440000L,
            null,
            false,
            null,
            null,
            1,
            1.0,
            null
        )
        val nextEpisode = mockkClass(AbstractCrunchyrollWrapper.Episode::class)
        every { nextEpisode.id } returns "nextId"
        every { nextEpisode.sequenceNumber } returns 2.0
        val expectedEpisode = mockkClass(AbstractCrunchyrollWrapper.BrowseObject::class)
        every { nextEpisode.convertToBrowseObject() } returns expectedEpisode

        mockkStatic(CrunchyrollWrapper::class) {
            coEvery { CrunchyrollWrapper.getUpNext(any(String::class), any(String::class)) } throws Exception()
            coEvery { CrunchyrollWrapper.getJvmStaticEpisode(any(String::class), any(String::class)) } returns episode
            coEvery { CrunchyrollWrapper.getJvmStaticEpisodesBySeasonId(any(String::class), any(String::class)) } returns listOf(nextEpisode)
            coEvery { CrunchyrollWrapper.getJvmStaticObjects(any(String::class), any(String::class)) } returns listOf(expectedEpisode)

            val result = runBlocking { platform.getNextEpisode(countryCode, crunchyrollId) }
            assertEquals(expectedEpisode, result)
        }
    }

    @Test
    fun getNextEpisodeNotFound() = runBlocking {
        val countryCode = CountryCode.FR
        val crunchyrollId = "someId"
        val episode = AbstractCrunchyrollWrapper.Episode(
            "",
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            StringUtils.EMPTY_STRING,
            emptyList(),
            ZonedDateTime.now().minusDays(1),
            "seasonId",
            null,
            null,
            StringUtils.EMPTY_STRING,
            null,
            null,
            null,
            null,
            1440000L,
            null,
            false,
            null,
            null,
            1,
            1.0,
            null
        )

        mockkStatic(CrunchyrollWrapper::class) {
            coEvery { CrunchyrollWrapper.getUpNext(any(String::class), any(String::class)) } throws Exception()
            coEvery { CrunchyrollWrapper.getJvmStaticEpisode(any(String::class), any(String::class)) } returns episode
            coEvery { CrunchyrollWrapper.getJvmStaticEpisodesBySeasonId(any(String::class), any(String::class)) } returns listOf()

            val result = runBlocking { platform.getNextEpisode(countryCode, crunchyrollId) }
            assertNull(result)
        }
    }
}