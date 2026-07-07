package fr.shikkanime.fetchers

import fr.shikkanime.builders.impl.BrowseObjectMockKBuilder
import fr.shikkanime.builders.impl.VersionMockKBuilder
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Locale
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.platforms.configuration.CrunchyrollConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime

@ExtendWith(MockKExtension::class)
class CrunchyrollEpisodeFetcherTest {
    private val countryCode = CountryCode.FR

    @MockK private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @MockK private lateinit var configCacheService: ConfigCacheService
    @MockK private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @InjectMockKs private lateinit var crunchyrollEpisodeFetcher: CrunchyrollEpisodeFetcher

    @BeforeEach
    fun setUp() {
        val crunchyrollConfiguration = CrunchyrollConfiguration()
        crunchyrollConfiguration.availableCountries = listOf(countryCode)

        every { crunchyrollPlatform.configuration } returns crunchyrollConfiguration
        every { crunchyrollPlatform.getPlatform() } returns Platform.CRUN

        mockkObject(CrunchyrollCachedWrapper, CrunchyrollWrapper, StringUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("preloadVariantsAndSeries")
    inner class PreloadVariantsAndSeriesTests {
        @Test
        suspend fun `should preload variants and series when versions have different audio locales`() {
            // Given
            val seriesId = "123"
            val japaneseVersionId = "1"
            val frenchVersionId = "2"

            val japaneseVersion = VersionMockKBuilder {
                audioLocale = Locale.JA_JP.code
                guid = japaneseVersionId
            }.build()

            val frenchVersion = VersionMockKBuilder {
                audioLocale = Locale.FR_FR.code
                guid = frenchVersionId
            }.build()

            val japaneseEpisode = BrowseObjectMockKBuilder {
                id = japaneseVersionId
                episode {
                    this.seriesId = seriesId
                    versions = listOf(japaneseVersion, frenchVersion)
                }
            }.build()

            val frenchEpisode = BrowseObjectMockKBuilder {
                id = frenchVersionId
                episode {
                    this.seriesId = seriesId
                    versions = listOf(japaneseVersion, frenchVersion)
                }
            }.build()

            coEvery { CrunchyrollCachedWrapper.getObjects(countryCode.locale, *arrayOf(frenchVersionId, seriesId)) } returns listOf(frenchEpisode)

            // When
            val result = crunchyrollEpisodeFetcher.preloadVariantsAndSeries(countryCode, listOf(japaneseEpisode))

            // Then
            coVerify { CrunchyrollCachedWrapper.getObjects(countryCode.locale, *arrayOf(frenchVersionId, seriesId)) }
            assertEquals(setOf(frenchEpisode), result)
        }
    }

    @Nested
    @DisplayName("fetchUpcomingEpisodes")
    inner class FetchUpcomingEpisodesTests {
        @Test
        suspend fun `should fetch upcoming episodes when there is a valid previous week and no simulcast`() {
            // Given
            val previousWeekToCheck = 1L
            val now = ZonedDateTime.now()
            val pair = "identifier" to now.minusWeeks(previousWeekToCheck)
            val videoId = "videoId"
            val nextEpisode = BrowseObjectMockKBuilder().build()

            coEvery { configCacheService.getValueAsLong(ConfigPropertyKey.PREDICT_FUTURE_EPISODES_WEEKS, any()) } returns previousWeekToCheck
            coEvery { configCacheService.getValueAsBoolean(ConfigPropertyKey.CRUNCHYROLL_CHECK_SERIES_SIMULCAST, any()) } returns false

            coEvery { episodeVariantCacheService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(countryCode, crunchyrollPlatform.getPlatform(), any(), any()) } returns arrayOf(pair)
            every { StringUtils.getVideoOldIdOrId(pair.first) } returns videoId
            coEvery { CrunchyrollWrapper.retrieveNextEpisode(countryCode.locale, videoId) } returns nextEpisode

            // When
            val result = crunchyrollEpisodeFetcher.fetchUpcomingEpisodes(countryCode, crunchyrollPlatform, now, false, emptyList())

            // Then
            verify { StringUtils.getVideoOldIdOrId(pair.first) }
            coVerify { CrunchyrollWrapper.retrieveNextEpisode(countryCode.locale, videoId) }
            assertEquals(setOf(nextEpisode), result)
        }

        @Test
        suspend fun `should not fetch upcoming episodes when the current time is 30 minutes after the release date`() {
            // Given
            val previousWeekToCheck = 1L
            val now = ZonedDateTime.now()
            val pair = "identifier" to now.minusWeeks(previousWeekToCheck)

            coEvery { configCacheService.getValueAsLong(ConfigPropertyKey.PREDICT_FUTURE_EPISODES_WEEKS, any()) } returns previousWeekToCheck
            coEvery { configCacheService.getValueAsBoolean(ConfigPropertyKey.CRUNCHYROLL_CHECK_SERIES_SIMULCAST, any()) } returns false

            coEvery { episodeVariantCacheService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(countryCode, crunchyrollPlatform.getPlatform(), any(), any()) } returns arrayOf(pair)

            // When
            val result = crunchyrollEpisodeFetcher.fetchUpcomingEpisodes(countryCode, crunchyrollPlatform, now.plusMinutes(30), false, emptyList())

            // Then
            coVerify(exactly = 0) { StringUtils.getVideoOldIdOrId(pair.first) }
            coVerify(exactly = 0) { CrunchyrollWrapper.retrieveNextEpisode(countryCode.locale, any()) }
            assertTrue(result.isEmpty())
        }
    }
}