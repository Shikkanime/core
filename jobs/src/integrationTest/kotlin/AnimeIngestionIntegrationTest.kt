package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformAnimeLocalization
import fr.shikkanime.jobs.platforms.PlatformImage
import fr.shikkanime.models.Country
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

/**
 * End-to-end runs of the harvest persistence against a real database.
 *
 * These tests boot the application through [bootstrap] and let every bean be the real one, so a
 * wiring problem surfaces here instead of during a manual run: a service consumed without its
 * transactional proxy, a query outside a transaction, a mapping reading a lazy relation twice.
 * Mocked unit tests cannot see any of those.
 */
class AnimeIngestionIntegrationTest {

    companion object {
        /** The application graph is global: boot it once for the whole run. */
        private val koin by lazy { bootstrap() }
    }

    /**
     * Persists a freshly harvested anime and reads it back through the same stack.
     */
    @Test
    fun `should persist a harvested anime through the real application bootstrap`() {
        // Given
        DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1").apply {
            databaseWrapper.connect()
            databaseWrapper.initializeSchema()
        }

        // When
        val saved = koin.get<AnimeIngestionService>()
            .saveAll(Platform.ANIMATION_DIGITAL_NETWORK, listOf(harvestedAnime()))

        // Then
        assertEquals(1, saved)
        val found = koin.get<AnimeService>()
            .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")

        assertNotNull(found)
        assertEquals("Title", found.localizations.single().title)
        assertEquals("https://cdn.example/thumbnail.jpg", found.images.single().url)
    }

    /**
     * Harvests the same anime twice and verifies the stored row is updated, not duplicated.
     */
    @Test
    fun `should update the stored anime when the same platform link is harvested again`() {
        // Given
        DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1").apply {
            databaseWrapper.connect()
            databaseWrapper.initializeSchema()
        }
        val ingestion = koin.get<AnimeIngestionService>()

        // When
        ingestion.saveAll(Platform.ANIMATION_DIGITAL_NETWORK, listOf(harvestedAnime()))
        ingestion.saveAll(
            Platform.ANIMATION_DIGITAL_NETWORK,
            listOf(harvestedAnime(localizationTitle = "Renamed"))
        )

        // Then
        val found = koin.get<AnimeService>()
            .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")

        assertNotNull(found)
        assertEquals("Renamed", found.localizations.single().title)
        assertEquals(1, koin.get<AnimeService>().findAll().size)
    }

    /**
     * Builds one harvested anime exactly as a streaming platform produces it.
     *
     * @param localizationTitle title carried by the harvested localization.
     * @return the normalized harvest to persist.
     */
    private fun harvestedAnime(localizationTitle: String = "Title"): PlatformAnime =
        PlatformAnime(
            id = "123",
            platform = Platform.ANIMATION_DIGITAL_NETWORK,
            images = listOf(
                PlatformImage(type = ImageType.THUMBNAIL, url = "https://cdn.example/thumbnail.jpg")
            ),
            localizations = listOf(
                PlatformAnimeLocalization(
                    country = Country.FR,
                    locale = Country.FR.defaultLocale,
                    title = localizationTitle,
                    description = "Description"
                )
            )
        )
}
