package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.*
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
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

    @Inject
    private lateinit var configService: ConfigService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
        configService.save(Config(propertyKey = ConfigPropertyKey.SIMULCAST_RANGE.key, propertyValue = "10"))
        configService.save(Config(propertyKey = ConfigPropertyKey.SIMULCAST_RANGE_DELAY.key, propertyValue = "3"))
    }

    @AfterEach
    fun tearDown() {
        configService.deleteAll()
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
                banner = "https://www.shikkanime.com/image.png",
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

        val simulcast = episodeService.getSimulcast(episode.anime!!, episode)
        assertEquals("WINTER", simulcast.season)
        assertEquals(2024, simulcast.year)

        val savedEpisode = episodeService.save(episode)
        val simulcasts = savedEpisode.anime!!.simulcasts.sortBySeasonAndYear()
        assertEquals(1, simulcasts.size)
        assertEquals("WINTER", simulcasts.first().season)
        assertEquals(2024, simulcasts.first().year)
    }

    @Test
    fun `get autumn simulcast`() {
        episodeService.save(
            Episode(
                platform = Platform.CRUN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "Test",
                    image = "https://www.shikkanime.com/image.png",
                    banner = "https://www.shikkanime.com/image.png",
                    releaseDateTime = ZonedDateTime.parse("2023-12-20T00:00:00Z"),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash",
                releaseDateTime = ZonedDateTime.parse("2023-12-25T00:00:00Z"),
                season = 1,
                number = 1,
                url = "https://www.shikkanime.com/episode/1",
                image = "https://www.shikkanime.com/image.png",
                duration = 1420,
            )
        )

        val episode = Episode(
            platform = Platform.CRUN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "Test",
                image = "https://www.shikkanime.com/image.png",
                banner = "https://www.shikkanime.com/image.png",
                releaseDateTime = ZonedDateTime.parse("2023-12-20T00:00:00Z"),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash-2",
            releaseDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
            season = 1,
            number = 2,
            url = "https://www.shikkanime.com/episode/1",
            image = "https://www.shikkanime.com/image.png",
            duration = 1420,
        )

        val simulcast = episodeService.getSimulcast(episode.anime!!, episode)
        assertEquals("WINTER", simulcast.season)
        assertEquals(2024, simulcast.year)
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
                banner = "https://www.shikkanime.com/image.png",
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
        val simulcasts = episode.anime!!.simulcasts.sortBySeasonAndYear()
        assertEquals("WINTER", simulcasts.first().season)
        assertEquals(2024, simulcasts.first().year)
    }

    @Test
    fun findDeprecatedEpisodes() {
        val releaseDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z")

        episodeService.save(
            Episode(
                platform = Platform.CRUN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "Test",
                    image = "https://www.shikkanime.com/image.png",
                    banner = "https://www.shikkanime.com/image.png",
                    releaseDateTime = releaseDateTime,
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-1",
                releaseDateTime = releaseDateTime,
                season = 1,
                number = 1,
                url = "https://www.shikkanime.com/episode/1",
                image = "https://www.shikkanime.com/image.png",
                duration = 1420,
                description = null,
                lastUpdateDateTime = null,
            )
        )

        episodeService.save(
            Episode(
                platform = Platform.CRUN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "Test",
                    image = "https://www.shikkanime.com/image.png",
                    banner = "https://www.shikkanime.com/image.png",
                    releaseDateTime = releaseDateTime,
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-2",
                releaseDateTime = releaseDateTime,
                season = 1,
                number = 2,
                url = "https://www.shikkanime.com/episode/1",
                image = "https://www.shikkanime.com/image.png",
                duration = 1420,
                description = "Test",
                lastUpdateDateTime = releaseDateTime.minusYears(1),
            )
        )

        episodeService.save(
            Episode(
                platform = Platform.CRUN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "Test",
                    image = "https://www.shikkanime.com/image.png",
                    banner = "https://www.shikkanime.com/image.png",
                    releaseDateTime = releaseDateTime,
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-3",
                releaseDateTime = releaseDateTime,
                season = 1,
                number = 3,
                url = "https://www.shikkanime.com/episode/1",
                image = "https://www.shikkanime.com/image.png",
                duration = 1420,
                description = "",
                lastUpdateDateTime = releaseDateTime,
            )
        )

        episodeService.save(
            Episode(
                platform = Platform.CRUN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "Test",
                    image = "https://www.shikkanime.com/image.png",
                    banner = "https://www.shikkanime.com/image.png",
                    releaseDateTime = releaseDateTime,
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-4",
                releaseDateTime = releaseDateTime,
                season = 1,
                number = 4,
                url = "https://www.shikkanime.com/episode/1",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                duration = 1420,
                description = "test",
                lastUpdateDateTime = releaseDateTime,
            )
        )

        val deprecatedEpisodes =
            episodeService.findAllByPlatformDeprecatedEpisodes(Platform.CRUN, releaseDateTime.minusDays(30))
        assertEquals(4, deprecatedEpisodes.size)
    }

    @Test
    fun `current simulcast on part 2 episode 13 delayed`() {
        episodeService.save(
            Episode(
                platform = Platform.DISN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "SYNDUALITY Noir",
                    image = Constant.DEFAULT_IMAGE_PREVIEW,
                    banner = Constant.DEFAULT_IMAGE_PREVIEW,
                    releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-10",
                releaseDateTime = ZonedDateTime.parse("2023-09-11T15:30:00Z"),
                season = 1,
                number = 10,
                url = "https://www.shikkanime.com/episode/1",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                duration = 1445,
            )
        )

        episodeService.save(
            Episode(
                platform = Platform.DISN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "SYNDUALITY Noir",
                    image = Constant.DEFAULT_IMAGE_PREVIEW,
                    banner = Constant.DEFAULT_IMAGE_PREVIEW,
                    releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-11",
                releaseDateTime = ZonedDateTime.parse("2023-09-18T15:30:00Z"),
                season = 1,
                number = 11,
                url = "https://www.shikkanime.com/episode/1",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                duration = 1445,
            )
        )

        val episode12 = Episode(
            platform = Platform.DISN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "SYNDUALITY Noir",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                banner = Constant.DEFAULT_IMAGE_PREVIEW,
                releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash-12",
            releaseDateTime = ZonedDateTime.parse("2023-09-25T15:30:00Z"),
            season = 1,
            number = 12,
            url = "https://www.shikkanime.com/episode/1",
            image = Constant.DEFAULT_IMAGE_PREVIEW,
            duration = 1445,
        )

        episodeService.save(episode12)
        val episode12Simulcasts = episode12.anime!!.simulcasts.sortBySeasonAndYear()
        assertEquals("SUMMER", episode12Simulcasts.first().season)
        assertEquals(2023, episode12Simulcasts.first().year)

        val episode13 = Episode(
            platform = Platform.DISN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "SYNDUALITY Noir",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                banner = Constant.DEFAULT_IMAGE_PREVIEW,
                releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash-13",
            releaseDateTime = ZonedDateTime.parse("2024-01-08T15:30:00Z"),
            season = 1,
            number = 13,
            url = "https://www.shikkanime.com/episode/1",
            image = Constant.DEFAULT_IMAGE_PREVIEW,
            duration = 1445,
        )

        episodeService.save(episode13)
        val episode13Simulcasts = episode13.anime!!.simulcasts.sortBySeasonAndYear()
        assertEquals("WINTER", episode13Simulcasts.first().season)
        assertEquals(2024, episode13Simulcasts.first().year)

        val episode14 = Episode(
            platform = Platform.DISN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "SYNDUALITY Noir",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                banner = Constant.DEFAULT_IMAGE_PREVIEW,
                releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash-14",
            releaseDateTime = ZonedDateTime.parse("2024-01-15T15:30:00Z"),
            season = 1,
            number = 14,
            url = "https://www.shikkanime.com/episode/1",
            image = Constant.DEFAULT_IMAGE_PREVIEW,
            duration = 1445,
        )

        episodeService.save(episode14)
        val episode14Simulcasts = episode14.anime!!.simulcasts.sortBySeasonAndYear()
        assertEquals(true, episode14Simulcasts.none { it.season == "AUTUMN" && it.year == 2023 })
    }

    @Test
    fun `current simulcast on episode 12`() {
        episodeService.save(
            Episode(
                platform = Platform.DISN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "SYNDUALITY Noir",
                    image = Constant.DEFAULT_IMAGE_PREVIEW,
                    banner = Constant.DEFAULT_IMAGE_PREVIEW,
                    releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-10",
                releaseDateTime = ZonedDateTime.parse("2023-09-11T15:30:00Z"),
                season = 1,
                number = 10,
                url = "https://www.shikkanime.com/episode/1",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                duration = 1445,
            )
        )

        episodeService.save(
            Episode(
                platform = Platform.DISN,
                anime = Anime(
                    countryCode = CountryCode.FR,
                    name = "SYNDUALITY Noir",
                    image = Constant.DEFAULT_IMAGE_PREVIEW,
                    banner = Constant.DEFAULT_IMAGE_PREVIEW,
                    releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
                ),
                episodeType = EpisodeType.EPISODE,
                langType = LangType.SUBTITLES,
                hash = "hash-11",
                releaseDateTime = ZonedDateTime.parse("2023-09-18T15:30:00Z"),
                season = 1,
                number = 11,
                url = "https://www.shikkanime.com/episode/1",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                duration = 1445,
            )
        )

        val previousEpisode = Episode(
            platform = Platform.DISN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "SYNDUALITY Noir",
                image = Constant.DEFAULT_IMAGE_PREVIEW,
                banner = Constant.DEFAULT_IMAGE_PREVIEW,
                releaseDateTime = ZonedDateTime.parse("2023-07-10T15:30:00Z"),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "hash-12",
            releaseDateTime = ZonedDateTime.parse("2023-09-25T15:30:00Z"),
            season = 1,
            number = 12,
            url = "https://www.shikkanime.com/episode/1",
            image = Constant.DEFAULT_IMAGE_PREVIEW,
            duration = 1445,
        )

        episodeService.save(previousEpisode)
        val previousEpisodeSimulcasts = previousEpisode.anime!!.simulcasts.sortBySeasonAndYear()
        assertEquals("SUMMER", previousEpisodeSimulcasts.first().season)
        assertEquals(2023, previousEpisodeSimulcasts.first().year)
    }
}