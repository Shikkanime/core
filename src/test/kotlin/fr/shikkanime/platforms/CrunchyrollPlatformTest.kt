package fr.shikkanime.platforms

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.ConfigService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZonedDateTime

class CrunchyrollPlatformTest {
    @Inject
    lateinit var platform: CrunchyrollPlatform

    @Inject
    lateinit var configService: ConfigService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        platform.loadConfiguration()
        platform.configuration!!.availableCountries.add(CountryCode.FR)
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
        MapCache.invalidate(Config::class.java)
    }

    @Test
    fun fetchEpisodesJSON() {
        val s = "2024-01-24T18:45:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.simulcasts[CountryCode.FR] = setOf("metallic rouge")

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

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

        platform.simulcasts[CountryCode.FR] = setOf("one piece")

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isNotEmpty())
        assertEquals("One Piece", episodes[0].anime)
        assertEquals(EpisodeType.SPECIAL, episodes[0].episodeType)
        assertEquals(13, episodes[0].number)
    }

    @Test
    fun `fetchEpisodes for 2024-06-08`() {
        val s = "2024-06-08T12:45:00Z"
        val zonedDateTime = ZonedDateTime.parse(s)

        platform.simulcasts[CountryCode.FR] = setOf("kaiju no. 8")

        val episodes = platform.fetchEpisodes(
            zonedDateTime,
            File(
                ClassLoader.getSystemClassLoader().getResource("crunchyroll/api-${s.replace(':', '-')}.json")?.file
                    ?: throw Exception("File not found")
            )
        )

        assertEquals(true, episodes.isNotEmpty())
        assertEquals("Kaiju No. 8", episodes[0].anime)
        assertEquals(EpisodeType.EPISODE, episodes[0].episodeType)
        assertEquals(9, episodes[0].number)
        assertEquals(Constant.DEFAULT_IMAGE_PREVIEW, episodes[0].image)
    }
}