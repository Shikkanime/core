package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration.AnimationDigitalNetworkSimulcast
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.InvalidationService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.time.ZonedDateTime
import java.util.stream.Stream

class AnimationDigitalNetworkPlatformTest : AbstractTest() {
    @Inject lateinit var platform: AnimationDigitalNetworkPlatform

    data class EpisodeTestCase(
        val date: String,
        val useApiFile: Boolean = true,
        val simulcasts: List<String> = emptyList(),
        val needsSimulcastDetectionRegexConfig: Boolean = false,
        val assertions: (List<AbstractPlatform.Episode>) -> Unit
    )

    companion object {
        @JvmStatic
        private fun adnTestCases(): Stream<EpisodeTestCase> = Stream.of(
            EpisodeTestCase(
                date = "2023-12-05T21:59:59Z",
                useApiFile = false,
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals(2, episodes.size)
                    assertEquals("Paradox Live THE ANIMATION", episodes[0].anime)
                    assertNotNull(episodes[0].description)
                    assertEquals("Helck", episodes[1].anime)
                    assertNotNull(episodes[1].description)
                }
            ),
            EpisodeTestCase(
                date = "2024-01-05T21:59:59Z",
                useApiFile = false,
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals(1, episodes.size)
                    assertEquals("Pon no Michi", episodes[0].anime)
                    assertNotNull(episodes[0].description)
                }
            ),
            EpisodeTestCase(
                date = "2024-01-21T21:59:59Z",
                useApiFile = false,
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals(5, episodes.size)
                    val onePieceEpisodes = episodes.filter { it.anime == "One Piece" }
                    assertEquals(2, onePieceEpisodes.size)
                    assertTrue(onePieceEpisodes.any { it.audioLocale == "ja-JP" })
                    assertTrue(onePieceEpisodes.any { it.audioLocale == "fr-FR" })
                    assertTrue(episodes.any { it.anime == "Run For Money" })
                    val monstersEpisodes = episodes.filter { it.anime == "MONSTERS" }
                    assertEquals(2, monstersEpisodes.size)
                    assertTrue(monstersEpisodes.any { it.audioLocale == "ja-JP" })
                    assertTrue(monstersEpisodes.any { it.audioLocale == "fr-FR" })
                }
            ),
            EpisodeTestCase(
                date = "2022-12-09T23:59:59Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals(1, episodes.size)
                    assertEquals("My Master Has No Tail", episodes[0].anime)
                    assertNotNull(episodes[0].description)
                }
            ),
            EpisodeTestCase(
                date = "2022-03-18T23:59:59Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals(1, episodes.size)
                    assertEquals("Les Héros de la Galaxie : Die Neue These", episodes[0].anime)
                    assertNotNull(episodes[0].description)
                }
            ),
            EpisodeTestCase(
                date = "2024-02-01T23:59:59Z",
                useApiFile = false,
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals(4, episodes.size)
                    assertTrue(episodes.any { it.anime == "Demon Slave" })
                    assertTrue(episodes.any { it.anime == "My Instant Death Ability Is So Overpowered, No One in This Other World Stands a Chance Against Me!" })
                    assertTrue(episodes.any { it.anime == "Urusei Yatsura" })
                }
            ),
            EpisodeTestCase(
                date = "2024-04-10T08:00:00Z",
                needsSimulcastDetectionRegexConfig = true,
                assertions = { episodes ->
                    assertTrue(episodes.isEmpty())
                }
            ),
            EpisodeTestCase(
                date = "2024-04-14T09:00:00Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertEquals("One Piece", episodes[0].anime)
                    assertEquals(EpisodeType.SPECIAL, episodes[0].episodeType)
                    assertEquals(13, episodes[0].number)
                }
            ),
            EpisodeTestCase(
                date = "2023-07-11T23:59:59Z",
                assertions = { episodes ->
                    assertTrue(episodes.isEmpty())
                }
            ),
            EpisodeTestCase(
                date = "2022-08-06T23:59:59Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertTrue(episodes.any { it.anime == "Dragon Quest - The Adventures of Dai" })
                    assertTrue(episodes.any { it.anime == "Kingdom" })
                }
            ),
            EpisodeTestCase(
                date = "2024-07-07T09:30:00Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertTrue(episodes.any { it.anime == "FAIRY TAIL 100 YEARS QUEST" })
                }
            ),
            EpisodeTestCase(
                date = "2022-09-27T23:59:59Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertTrue(episodes.any { it.anime == "Overlord" })
                }
            ),
            EpisodeTestCase(
                date = "2025-04-20T07:00:00Z",
                useApiFile = false,
                simulcasts = listOf("Witch Watch"),
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertTrue(episodes.any { it.anime == "WITCH WATCH" })
                }
            ),
            EpisodeTestCase(
                date = "2025-05-09T07:00:00Z",
                useApiFile = false,
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    val animeEpisodes = episodes.filter { it.anime == "OVERLORD: The Sacred Kingdom" }
                    assertEquals(2, animeEpisodes.size)
                    assertEquals(EpisodeType.FILM, animeEpisodes.first().episodeType)
                }
            ),
            EpisodeTestCase(
                date = "2025-06-25T07:00:00Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertTrue(episodes.none { it.anime == "Eyeshield 2" })
                    val animeEpisodes = episodes.filter { it.anime == "Eyeshield 21" }
                    assertEquals(290, animeEpisodes.size)
                }
            ),
            EpisodeTestCase(
                date = "2025-12-18T16:30:00Z",
                assertions = { episodes ->
                    assertTrue(episodes.isNotEmpty())
                    assertTrue(episodes.any { it.anime == "Dusk Beyond the End of the World" })
                    assertEquals(1, episodes.size)
                    val episode = episodes.first()
                    assertEquals(episode.image, Constant.DEFAULT_IMAGE_PREVIEW)
                }
            )
        )
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()

        platform.loadConfiguration()
        platform.configuration!!.availableCountries = mutableSetOf(CountryCode.FR)
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Pon no Michi" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "One Piece" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Urusei Yatsura" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Dragon Quest - The Adventures of Dai" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Kingdom" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Demon Slave" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Overlord" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Eyeshield 21" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Les Héros de la Galaxie : Die Neue These" })
        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = "Dusk Beyond the End of the World" })
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        platform.configuration!!.availableCountries = emptySet()
        platform.configuration!!.simulcasts.clear()
        platform.reset()
    }

    private fun setupSimulcastDetectionRegexConfig() {
        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX.key,
                propertyValue = "\\((premier épisode |diffusion des épisodes |diffusion du premier épisode|diffusion de l'épisode 1 le)"
            )
        )
        InvalidationService.invalidate(Config::class.java)
    }

    @ParameterizedTest
    @MethodSource("adnTestCases")
    fun fetchEpisodes(testCase: EpisodeTestCase) {
        if (testCase.needsSimulcastDetectionRegexConfig) {
            setupSimulcastDetectionRegexConfig()
        }

        testCase.simulcasts.forEach {
            platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast().apply { name = it })
        }

        val zonedDateTime = ZonedDateTime.parse(testCase.date)

        val episodes = runBlocking {
            platform.fetchEpisodes(
                zonedDateTime,
                (ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${testCase.date.replace(':', '-')}.json")?.file
                    ?.let { File(it) }).takeIf { testCase.useApiFile },
            )
        }

        testCase.assertions(episodes)
    }
}