package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration.AnimationDigitalNetworkSimulcast
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZonedDateTime

class AnimationDigitalNetworkPlatformTest : AbstractTest() {
    @Inject lateinit var platform: AnimationDigitalNetworkPlatform

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
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        platform.configuration!!.availableCountries = emptySet()
        platform.configuration!!.simulcasts.clear()
        platform.reset()
    }

    @Test
    fun `fetchEpisodes for 2023-12-05`() {
        val zonedDateTime = ZonedDateTime.parse("2023-12-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(2, episodes.size)
        assertEquals("Paradox Live THE ANIMATION", episodes[0].anime)
        assertNotNull(episodes[0].description)
        assertEquals("Helck", episodes[1].anime)
        assertNotNull(episodes[1].description)
    }

    @Test
    fun `fetchEpisodes for 2024-01-05`() {
        val zonedDateTime = ZonedDateTime.parse("2024-01-05T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Pon no Michi", episodes[0].anime)
        assertNotNull(episodes[0].description)
    }

    @Test
    fun `fetchEpisodes for 2024-01-21`() {
        val zonedDateTime = ZonedDateTime.parse("2024-01-21T21:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)
        println(episodes)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(5, episodes.size)
        assertEquals("One Piece", episodes[0].anime)
        assertNotNull(episodes[0].description)
        assertEquals("ja-JP", episodes[0].audioLocale)
        assertEquals("fr-FR", episodes[1].audioLocale)

        assertEquals("Run For Money", episodes[2].anime)
        assertNotNull(episodes[2].description)

        assertEquals("MONSTERS", episodes[3].anime)
        assertEquals("ja-JP", episodes[3].audioLocale)
        assertNotNull(episodes[3].description)
        assertEquals("fr-FR", episodes[4].audioLocale)
        assertNotNull(episodes[4].description)
    }

    @Test
    fun `fetchEpisodes for 2022-12-09`() {
        val zonedDateTime = ZonedDateTime.parse("2022-12-09T23:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("My Master Has No Tail", episodes[0].anime)
        assertNotNull(episodes[0].description)
    }

    @Test
    fun `fetchEpisodes for 2022-03-18`() {
        val zonedDateTime = ZonedDateTime.parse("2022-03-18T23:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(1, episodes.size)
        assertEquals("Les Héros de la Galaxie : Die Neue These", episodes[0].anime)
        assertNotNull(episodes[0].description)
    }

    @Test
    fun `fetchEpisodes for 2024-02-01`() {
        val zonedDateTime = ZonedDateTime.parse("2024-02-01T23:59:59Z")
        val episodes = platform.fetchEpisodes(zonedDateTime)
        println(episodes)

        assertEquals(true, episodes.isNotEmpty())
        assertEquals(4, episodes.size)
        assertEquals("Demon Slave", episodes[0].anime)
        assertNotNull(episodes[0].description)
        assertEquals(
            "My Instant Death Ability Is So Overpowered, No One in This Other World Stands a Chance Against Me!",
            episodes[1].anime
        )
        assertNotNull(episodes[1].description)
        assertEquals("Urusei Yatsura", episodes[2].anime)
        assertNotNull(episodes[2].description)
    }

    @Test
    fun `fetchEpisodes for 2024-04-10`() {
        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX.key,
                propertyValue = "\\((premier épisode |diffusion des épisodes |diffusion du premier épisode|diffusion de l'épisode 1 le)"
            )
        )
        MapCache.invalidate(Config::class.java)

        val s = "2024-04-10T08:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isEmpty())
    }

    @Test
    fun `fetchEpisodes for 2024-04-14`() {
        val s = "2024-04-14T09:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isNotEmpty())
        assertEquals("One Piece", episodes[0].anime)
        assertEquals(EpisodeType.SPECIAL, episodes[0].episodeType)
        assertEquals(13, episodes[0].number)
    }

    @Test
    fun `fetchEpisodes for 2023-07-11`() {
        val s = "2023-07-11T23:59:59Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isEmpty())
    }

    @Test
    fun `fetchEpisodes for 2022-08-06`() {
        val s = "2022-08-06T23:59:59Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        episodes.forEach(::println)

        assertTrue(episodes.isNotEmpty())
        assertTrue(episodes.any { it.anime == "Dragon Quest - The Adventures of Dai" })
        assertTrue(episodes.any { it.anime == "Kingdom" })
    }

    @Test
    fun `fetchEpisodes for 2024-07-07`() {
        val s = "2024-07-07T09:30:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        println(episodes)

        assertTrue(episodes.isNotEmpty())
        assertTrue(episodes.any { it.anime == "FAIRY TAIL 100 YEARS QUEST" })
    }

    @Test
    fun `fetchEpisodes for 2022-09-27`() {
        val s = "2022-09-27T23:59:59Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader()
                    .getResource("animation_digital_network/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        println(episodes)

        assertTrue(episodes.isNotEmpty())
        assertTrue(episodes.any { it.anime == "Overlord" })
    }

    @Test
    fun `fetchEpisodes for 2025-04-20`() {
        val s = "2025-04-20T07:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.configuration!!.simulcasts.add(AnimationDigitalNetworkSimulcast(2L).apply { name = "Witch Watch" })
        val episodes = platform.fetchEpisodes(zonedDateTime)

        println(episodes)

        assertTrue(episodes.isNotEmpty())
        assertTrue(episodes.any { it.anime == "WITCH WATCH" })
    }

    @Test
    fun `fetchEpisodes for 2025-05-09`() {
        val s = "2025-05-09T07:00:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)
        val episodes = platform.fetchEpisodes(zonedDateTime)

        println(episodes)

        assertTrue(episodes.isNotEmpty())
        assertTrue(episodes.any { it.anime == "OVERLORD: The Sacred Kingdom" })
        val animeEpisodes = episodes.filter { it.anime == "OVERLORD: The Sacred Kingdom" }
        assertEquals(2, animeEpisodes.size)
        assertEquals(EpisodeType.FILM, animeEpisodes.first().episodeType)
    }
}