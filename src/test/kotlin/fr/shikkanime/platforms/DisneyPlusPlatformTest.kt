package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.caches.CountryCodeReleaseDayPlatformSimulcastKeyCache
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.platforms.configuration.ReleaseDayPlatformSimulcast
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

/**
 * Unit tests for DisneyPlusPlatform.
 * 
 * These tests verify error handling in the fetchEpisodes method to ensure that
 * when one show fails to fetch, other shows continue to be processed.
 */
class DisneyPlusPlatformTest : AbstractTest() {
    @Inject lateinit var platform: DisneyPlusPlatform

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
    fun `fetchEpisodes should continue processing other shows when one show throws exception`() {
        // Arrange: Set up two simulcasts (shows)
        val simulcast1 = ReleaseDayPlatformSimulcast(0, "show-id-1")
        val simulcast2 = ReleaseDayPlatformSimulcast(0, "show-id-2")
        platform.configuration!!.simulcasts.add(simulcast1)
        platform.configuration!!.simulcasts.add(simulcast2)

        val zonedDateTime = ZonedDateTime.parse("2025-10-22T08:00:00Z")
        val platformSpy = spyk(platform)

        // Mock getApiContent to throw exception for first show but return valid data for second
        val key1 = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, simulcast1)
        val key2 = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, simulcast2)

        // First show throws an exception
        every { platformSpy.getApiContent(key1, zonedDateTime) } throws RuntimeException("Test exception for show 1")

        // Second show returns valid episodes
        val validEpisode = AbstractPlatform.Episode(
            countryCode = CountryCode.FR,
            animeId = "show-id-2",
            anime = "Test Anime 2",
            animeAttachments = mapOf(
                ImageType.THUMBNAIL to "https://example.com/thumbnail.jpg",
                ImageType.BANNER to "https://example.com/banner.jpg",
                ImageType.CAROUSEL to "https://example.com/carousel.jpg"
            ),
            animeDescription = "Test description",
            releaseDateTime = zonedDateTime,
            episodeType = EpisodeType.EPISODE,
            seasonId = "season-1",
            season = 1,
            number = 1,
            duration = 1440,
            title = "Episode 1",
            description = "Episode 1 description",
            image = "https://example.com/episode.jpg",
            platform = platform.getPlatform(),
            audioLocale = "ja-JP",
            id = "episode-1",
            url = "https://example.com/watch/episode-1",
            uncensored = false,
            original = true
        )

        every { platformSpy.getApiContent(key2, zonedDateTime) } returns listOf(validEpisode)

        // Act: Call fetchEpisodes
        val result = platformSpy.fetchEpisodes(zonedDateTime, null)

        // Assert: Verify that only episodes from the second show are returned
        assertNotNull(result)
        assertEquals(1, result.size, "Should have 1 episode from the second show")
        assertEquals("show-id-2", result[0].animeId)
        assertEquals("Test Anime 2", result[0].anime)
        assertEquals("Episode 1", result[0].title)
    }

    @Test
    fun `fetchEpisodes should return empty list when all shows throw exceptions`() {
        // Arrange: Set up two simulcasts that both fail
        val simulcast1 = ReleaseDayPlatformSimulcast(0, "show-id-1")
        val simulcast2 = ReleaseDayPlatformSimulcast(0, "show-id-2")
        platform.configuration!!.simulcasts.add(simulcast1)
        platform.configuration!!.simulcasts.add(simulcast2)

        val zonedDateTime = ZonedDateTime.parse("2025-10-22T08:00:00Z")
        val platformSpy = spyk(platform)

        val key1 = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, simulcast1)
        val key2 = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, simulcast2)

        // Both shows throw exceptions
        every { platformSpy.getApiContent(key1, zonedDateTime) } throws RuntimeException("Test exception for show 1")
        every { platformSpy.getApiContent(key2, zonedDateTime) } throws NullPointerException("Test exception for show 2")

        // Act: Call fetchEpisodes
        val result = platformSpy.fetchEpisodes(zonedDateTime, null)

        // Assert: Verify that an empty list is returned (no crash)
        assertNotNull(result)
        assertEquals(0, result.size, "Should return empty list when all shows fail")
    }

    @Test
    fun `fetchEpisodes should process all shows successfully when no exceptions occur`() {
        // Arrange: Set up two simulcasts that both succeed
        val simulcast1 = ReleaseDayPlatformSimulcast(0, "show-id-1")
        val simulcast2 = ReleaseDayPlatformSimulcast(0, "show-id-2")
        platform.configuration!!.simulcasts.add(simulcast1)
        platform.configuration!!.simulcasts.add(simulcast2)

        val zonedDateTime = ZonedDateTime.parse("2025-10-22T08:00:00Z")
        val platformSpy = spyk(platform)

        val key1 = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, simulcast1)
        val key2 = CountryCodeReleaseDayPlatformSimulcastKeyCache(CountryCode.FR, simulcast2)

        val episode1 = AbstractPlatform.Episode(
            countryCode = CountryCode.FR,
            animeId = "show-id-1",
            anime = "Test Anime 1",
            animeAttachments = mapOf(
                ImageType.THUMBNAIL to "https://example.com/thumbnail1.jpg",
                ImageType.BANNER to "https://example.com/banner1.jpg",
                ImageType.CAROUSEL to "https://example.com/carousel1.jpg"
            ),
            animeDescription = "Test description 1",
            releaseDateTime = zonedDateTime,
            episodeType = EpisodeType.EPISODE,
            seasonId = "season-1",
            season = 1,
            number = 1,
            duration = 1440,
            title = "Episode 1",
            description = "Episode 1 description",
            image = "https://example.com/episode1.jpg",
            platform = platform.getPlatform(),
            audioLocale = "ja-JP",
            id = "episode-1",
            url = "https://example.com/watch/episode-1",
            uncensored = false,
            original = true
        )

        val episode2 = AbstractPlatform.Episode(
            countryCode = CountryCode.FR,
            animeId = "show-id-2",
            anime = "Test Anime 2",
            animeAttachments = mapOf(
                ImageType.THUMBNAIL to "https://example.com/thumbnail2.jpg",
                ImageType.BANNER to "https://example.com/banner2.jpg",
                ImageType.CAROUSEL to "https://example.com/carousel2.jpg"
            ),
            animeDescription = "Test description 2",
            releaseDateTime = zonedDateTime,
            episodeType = EpisodeType.EPISODE,
            seasonId = "season-1",
            season = 1,
            number = 1,
            duration = 1440,
            title = "Episode 2",
            description = "Episode 2 description",
            image = "https://example.com/episode2.jpg",
            platform = platform.getPlatform(),
            audioLocale = "ja-JP",
            id = "episode-2",
            url = "https://example.com/watch/episode-2",
            uncensored = false,
            original = true
        )

        every { platformSpy.getApiContent(key1, zonedDateTime) } returns listOf(episode1)
        every { platformSpy.getApiContent(key2, zonedDateTime) } returns listOf(episode2)

        // Act: Call fetchEpisodes
        val result = platformSpy.fetchEpisodes(zonedDateTime, null)

        // Assert: Verify that episodes from both shows are returned
        assertNotNull(result)
        assertEquals(2, result.size, "Should have episodes from both shows")
        assertEquals("show-id-1", result[0].animeId)
        assertEquals("show-id-2", result[1].animeId)
    }
}
