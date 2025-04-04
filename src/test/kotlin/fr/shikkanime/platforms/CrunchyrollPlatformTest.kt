package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.configuration.PlatformSimulcast
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZonedDateTime

class CrunchyrollPlatformTest : AbstractTest() {
    @Inject
    lateinit var platform: CrunchyrollPlatform

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

    @Test
    fun fetchEpisodesJSON() {
        val s = "2024-01-24T18:45:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(PlatformSimulcast(name = "metallic rouge"))

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "Metallic Rouge" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(2, episodes.size)
        assertEquals("Metallic Rouge", episodes[0].anime)
        assertEquals("fr-FR", episodes[0].audioLocale)
        assertNotNull(episodes[0].description)
        assertEquals("Metallic Rouge", episodes[1].anime)
        assertEquals("ja-JP", episodes[1].audioLocale)
        assertNotNull(episodes[1].description)
    }

    @Test
    fun `fetchEpisodes for 2024-04-14`() {
        val s = "2024-04-14T09:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(PlatformSimulcast(name = "one piece"))

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "One Piece" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals("One Piece", episodes[0].anime)
        assertEquals(EpisodeType.SPECIAL, episodes[0].episodeType)
        assertEquals(13, episodes[0].number)
    }

    @Test
    fun `fetchEpisodes for 2024-06-08`() {
        val s = "2024-06-08T12:45:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(PlatformSimulcast(name = "kaiju no. 8"))

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "Kaiju No. 8" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals("Kaiju No. 8", episodes[0].anime)
        assertEquals(EpisodeType.EPISODE, episodes[0].episodeType)
        assertEquals(9, episodes[0].number)
        assertEquals(Constant.DEFAULT_IMAGE_PREVIEW, episodes[0].image)
    }

    @Test
    fun `fetchEpisodes for 2024-07-08`() {
        val s = "2024-07-08T07:30:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(PlatformSimulcast(name = "days with my stepsister"))
        platform.configuration!!.simulcasts.add(PlatformSimulcast(name = "mayonaka punch"))

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "Days with My Stepsister" && it.anime != "Mayonaka Punch" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Days with My Stepsister", episodes[0].anime)
        assertEquals(EpisodeType.EPISODE, episodes[0].episodeType)
        assertEquals(1, episodes[0].season)
        assertEquals(1, episodes[0].number)
    }

    @Test
    fun `fetchEpisodes for 2024-07-17`() {
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

        val s = "2024-07-17T15:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(PlatformSimulcast(name = "alya sometimes hides her feelings in russian"))

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "Alya Sometimes Hides Her Feelings in Russian" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Alya Sometimes Hides Her Feelings in Russian", episodes[0].anime)
        assertEquals(EpisodeType.EPISODE, episodes[0].episodeType)
        assertEquals(1, episodes[0].season)
        assertEquals(3, episodes[0].number)
    }

    @Test
    fun fetchNextEpisodeSuccessfully() {
        val expectedEpisode = mockkClass(AbstractCrunchyrollWrapper.BrowseObject::class)

        mockkStatic(CrunchyrollWrapper::class) {
            every {
                runBlocking {
                    CrunchyrollWrapper.getUpNext(
                        any(String::class),
                        any(String::class)
                    )
                }
            } returns expectedEpisode
            val result = runBlocking { platform.getNextEpisode(CountryCode.FR, "someId") }
            assertEquals(expectedEpisode, result)
        }
    }

    @Test
    fun getNextEpisodeFallbackToEpisode() = runBlocking {
        val episode = AbstractCrunchyrollWrapper.Episode(
            null,
            "",
            "",
            "",
            emptyList(),
            ZonedDateTime.now(),
            "",
            null,
            null,
            "",
            null,
            null,
            null,
            null,
            1440000L,
            null,
            false,
            null,
            "nextId",
        )
        val expectedEpisode = mockkClass(AbstractCrunchyrollWrapper.BrowseObject::class)

        mockkStatic(CrunchyrollWrapper::class) {
            every {
                runBlocking {
                    CrunchyrollWrapper.getUpNext(
                        any(String::class),
                        any(String::class)
                    )
                }
            } throws Exception()
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticEpisode(
                        any(String::class),
                        any(String::class)
                    )
                }
            } returns episode
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticObjects(
                        any(String::class),
                        any(String::class)
                    )
                }
            } returns listOf(expectedEpisode)
            val result = runBlocking { platform.getNextEpisode(CountryCode.FR, "someId") }
            assertEquals(expectedEpisode, result)
        }
    }

    @Test
    fun getNextEpisodeFallbackToSeason() = runBlocking {
        val countryCode = CountryCode.FR
        val crunchyrollId = "someId"
        val episode = AbstractCrunchyrollWrapper.Episode(
            null,
            "",
            "",
            "",
            emptyList(),
            ZonedDateTime.now().minusDays(1),
            "seasonId",
            null,
            null,
            "",
            null,
            null,
            null,
            null,
            1440000L,
            null,
            false,
            null,
            null,
        )
        val nextEpisode = mockkClass(AbstractCrunchyrollWrapper.Episode::class)
        every { nextEpisode.id } returns "nextId"
        every { nextEpisode.premiumAvailableDate } returns ZonedDateTime.now()
        val expectedEpisode = mockkClass(AbstractCrunchyrollWrapper.BrowseObject::class)

        mockkStatic(CrunchyrollWrapper::class) {
            every {
                runBlocking {
                    CrunchyrollWrapper.getUpNext(
                        any(String::class),
                        any(String::class)
                    )
                }
            } throws Exception()
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticEpisode(
                        any(String::class),
                        any(String::class)
                    )
                }
            } returns episode
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticEpisodesBySeasonId(
                        any(String::class),
                        any(String::class),
                    )
                }
            } returns listOf(nextEpisode)
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticObjects(
                        any(String::class),
                        any(String::class)
                    )
                }
            } returns listOf(expectedEpisode)

            val result = runBlocking { platform.getNextEpisode(countryCode, crunchyrollId) }
            assertEquals(expectedEpisode, result)
        }
    }

    @Test
    fun getNextEpisodeNotFound() = runBlocking {
        val countryCode = CountryCode.FR
        val crunchyrollId = "someId"
        val episode = AbstractCrunchyrollWrapper.Episode(
            null,
            "",
            "",
            "",
            emptyList(),
            ZonedDateTime.now().minusDays(1),
            "seasonId",
            null,
            null,
            "",
            null,
            null,
            null,
            null,
            1440000L,
            null,
            false,
            null,
            null,
        )

        mockkStatic(CrunchyrollWrapper::class) {
            every {
                runBlocking {
                    CrunchyrollWrapper.getUpNext(
                        any(String::class),
                        any(String::class)
                    )
                }
            } throws Exception()
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticEpisode(
                        any(String::class),
                        any(String::class)
                    )
                }
            } returns episode
            every {
                runBlocking {
                    CrunchyrollWrapper.getJvmStaticEpisodesBySeasonId(
                        any(String::class),
                        any(String::class),
                    )
                }
            } returns listOf()

            val result = runBlocking { platform.getNextEpisode(countryCode, crunchyrollId) }
            assertNull(result)
        }
    }

    @Test
    fun `fetchEpisodes for 2024-10-24`() {
        val s = "2024-10-24T22:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "BOCCHI THE ROCK!" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(12, episodes.size)
        assertEquals("BOCCHI THE ROCK!", episodes[0].anime)
    }

    @Test
    fun `fetchEpisodes for 2024-10-25`() {
        val s = "2024-10-25T18:15:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        ).filterNot { it.anime != "Gridman Universe" }

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Gridman Universe", episodes[0].anime)
        assertEquals(EpisodeType.FILM, episodes[0].episodeType)
    }

    @Test
    fun `fetchEpisodes for 2025-03-28`() {
        val s = "2025-03-28T06:30:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertTrue(episodes.none { it.anime == "Teogonia" })
        assertTrue(episodes.none { it.anime == "Can a Boy-Girl Friendship Survive?" })
        assertTrue(episodes.none { it.anime == "The Brilliant Healer's New Life in the Shadows" })
    }
}