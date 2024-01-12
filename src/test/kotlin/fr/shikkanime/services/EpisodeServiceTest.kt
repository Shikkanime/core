package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class EpisodeServiceTest {
    @Inject
    private lateinit var episodeService: EpisodeService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        episodeService.deleteAll()
        animeService.deleteAll()
        simulcastService.deleteAll()
    }

    @Test
    fun `get winter simulcast`() {
        val releaseDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z")

        val episode = Episode(
            platform = Platform.CRUN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "Test",
                image = "https://www.shikkanime.com/image.png",
                releaseDateTime = releaseDateTime,
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash",
            releaseDateTime = releaseDateTime,
            season = 1,
            number = 1,
            url = "https://www.shikkanime.com/episode/1",
            image = "https://www.shikkanime.com/image.png",
            duration = 1420,
        )

        val simulcast = episodeService.getSimulcast(episode)
        assertEquals("WINTER", simulcast.season)
        assertEquals(2024, simulcast.year)
    }

    @Test
    fun `get autumn simulcast`() {
        val episode = Episode(
            platform = Platform.CRUN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "Test",
                image = "https://www.shikkanime.com/image.png",
                releaseDateTime = ZonedDateTime.parse("2023-12-25T00:00:00Z"),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash",
            releaseDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            season = 1,
            number = 2,
            url = "https://www.shikkanime.com/episode/1",
            image = "https://www.shikkanime.com/image.png",
            duration = 1420,
        )

        val simulcast = episodeService.getSimulcast(episode)
        assertEquals("AUTUMN", simulcast.season)
        assertEquals(2023, simulcast.year)
    }

    @Test
    fun save() {
        val releaseDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z")

        val episode = Episode(
            platform = Platform.CRUN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "Test",
                image = "https://www.shikkanime.com/image.png",
                releaseDateTime = releaseDateTime,
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash",
            releaseDateTime = releaseDateTime,
            season = 1,
            number = 2,
            url = "https://www.shikkanime.com/episode/1",
            image = "https://www.shikkanime.com/image.png",
            duration = 1420,
        )

        episodeService.save(episode)
        assertEquals("WINTER", episode.anime!!.simulcasts.first().season)
        assertEquals(2024, episode.anime!!.simulcasts.first().year)
    }
}