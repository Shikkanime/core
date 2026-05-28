package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.builders.*
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UpdateEpisodeJobMockTest : AbstractTest() {
    @Inject private lateinit var updateEpisodeJob: UpdateEpisodeJob

    private val zonedDateTime = ZonedDateTime.now().minusMonths(6)

    @BeforeEach
    override suspend fun setUp() {
        super.setUp()
        mockkObject(CrunchyrollCachedWrapper)
        mockkObject(CrunchyrollWrapper)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        unmockkAll()
    }

    @Test
    suspend fun `should update rent a girlfriend episode with mocked crunchyroll platform conversion`() {
        // Given
        val anime = animeService.save(
            AnimeMockBuilder()
                .countryCode(CountryCode.FR)
                .name("Rent-a-Girlfriend")
                .slug("rent-a-girlfriend")
                .build()
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMappingMockBuilder()
                .anime(anime)
                .releaseDateTime(zonedDateTime)
                .season(1)
                .episodeType(EpisodeType.EPISODE)
                .number(1)
                .build()
        )

        val episodeVariant = episodeVariantService.save(
            EpisodeVariantMockBuilder()
                .mapping(episodeMapping)
                .platform(Platform.CRUN)
                .audioLocale("fr-FR")
                .identifier("FR-CRUN-GZ7UV8KWZ-FR-FR")
                .url("https://www.crunchyroll.com/fr/watch/GZ7UV8KWZ/rent-a-girlfriend")
                .build()
        )

        InvalidationService.invalidateAll()

        val browseObject = BrowserObjectMockBuilder()
            .id("GZ7UV8KWZ")
            .slugTitle("rent-a-girlfriend")
            .title("Petite amie à louer")
            .description("Entre tomber amoureux de Chizuru, la petite amie dont il a louer les services, ou de trois autres filles qui se battent pour lui, la vie amoureuse de Kazuya est hors de contrôle. Mais Chizuru envisage un changement de carrière. Cela sonne-t-il la fin de leur compromis amoureux ou est-ce le début de quelque chose de réel ?")
            .episode()
            .seriesId("G6QWV3976")
            .seriesTitle("Rent-a-Girlfriend")
            .audioLocale("fr-FR")
            .subtitleLocales(listOf("fr-FR", "ja-JP"))
            .premiumAvailableDate(zonedDateTime)
            .seasonId("G6VNC2ZQ9")
            .durationMs(1467009)
            .numberString("1")
            .build()

        val series = BrowserObjectMockBuilder()
            .id("G6QWV3976")
            .series()
            .isSimulcast(true)
            .build()

        coEvery { CrunchyrollWrapper.getObjects(any(), *varargAll<String> { it == browseObject.id }) } returns listOf(browseObject)
        coEvery { CrunchyrollWrapper.getObjects(any(), *varargAll<String> { it == series.id }) } returns listOf(series)
        coEvery { CrunchyrollCachedWrapper.retrievePreviousEpisode(any(), any()) } returns null
        coEvery { CrunchyrollCachedWrapper.retrieveNextEpisode(any(), any()) } returns null

        // When
        updateEpisodeJob.run()

        // Then
        val updatedEpisodeMapping = episodeMappingService.find(episodeMapping.uuid)
        val updatedEpisodeVariant = episodeVariantService.find(episodeVariant.uuid)
        assertNotNull(updatedEpisodeMapping)
        assertNotNull(updatedEpisodeVariant)
        assertTrue(updatedEpisodeMapping?.lastUpdateDateTime?.isAfter(zonedDateTime) ?: false)
        assertEquals(browseObject.title, updatedEpisodeMapping?.title)
        assertEquals(browseObject.description, updatedEpisodeMapping?.description)
        assertEquals(CrunchyrollWrapper.buildUrl(CountryCode.FR, browseObject.id, browseObject.slugTitle), updatedEpisodeVariant?.url)
    }
}