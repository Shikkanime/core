package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.framework.koin.applyTransactionalProxies
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.builders.impl.PlatformAnimeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformAnimeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformImageMockKBuilder
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.koin.plugin.module.dsl.startKoin
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

/**
 * End-to-end runs of the harvest persistence against a real database.
 *
 * These tests boot the application the way its own entrypoint does and let every bean be
 * the real one, so a wiring problem surfaces here instead of during a manual run: a service
 * consumed without its transactional proxy, a query outside a transaction, a mapping reading a
 * lazy relation twice.
 * Mocked unit tests cannot see any of those.
 */
@DisplayName("integration tests for the anime ingestion")
class AnimeIngestionIntegrationTest {

    companion object {
        private val koin by lazy {
            startKoin<MyApp>().koin.also(::applyTransactionalProxies)
        }
    }

    @Nested
    @DisplayName("tests for the harvest persistence through the real application bootstrap")
    inner class IngestionRunsTests {
        @Test
        @DisplayName("should persist a harvested anime through the real application bootstrap")
        fun shouldPersistAHarvestedAnimeThroughTheRealApplicationBootstrap() {
            // Given
            freshDatabase()

            // When
            val saved = koin.get<AnimeIngestionService>()
                .saveAll(listOf(harvestedEpisode(harvestedAnime(id = "123"), id = "3001")))

            // Then
            assertEquals(1, saved)
            val found = koin.get<AnimeService>()
                .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")

            assertNotNull(found)
            assertEquals("Title", found.localizations.single().title)
            assertEquals("https://cdn.example/thumbnail.jpg", found.images.single().url)
        }

        @Test
        @DisplayName("should update the stored anime when the same platform link is harvested again")
        fun shouldUpdateTheStoredAnimeWhenTheSamePlatformLinkIsHarvestedAgain() {
            // Given
            freshDatabase()
            val ingestion = koin.get<AnimeIngestionService>()

            // When
            ingestion.saveAll(listOf(harvestedEpisode(harvestedAnime(id = "456"), id = "5001")))
            ingestion.saveAll(
                listOf(
                    harvestedEpisode(
                        harvestedAnime(localizationTitle = "Title (2024)", id = "456"),
                        id = "5001"
                    )
                )
            )

            // Then
            val found = koin.get<AnimeService>()
                .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "456")

            assertNotNull(found)
            // The rules absorb a year in parentheses: same normalized title, so the row is updated
            assertEquals("Title (2024)", found.localizations.single().title)
            val storedAnimes = koin.get<AnimeService>()
                .findAll()
            assertEquals(1, storedAnimes.size)
        }

        @Test
        @DisplayName("should create a second anime when the identity name changes")
        fun shouldCreateASecondAnimeWhenTheIdentityNameChanges() {
            // Given
            freshDatabase()
            val ingestion = koin.get<AnimeIngestionService>()

            // When
            ingestion.saveAll(listOf(harvestedEpisode(harvestedAnime(id = "789"), id = "7001")))
            ingestion.saveAll(
                listOf(
                    harvestedEpisode(
                        harvestedAnime(localizationTitle = "Renamed", id = "789"),
                        id = "7001"
                    )
                )
            )

            // Then: a rename is a new identity name, so a new anime - both are kept and may be merged
            val storedAnimes = koin.get<AnimeService>()
                .findAll()
            assertEquals(2, storedAnimes.size)
        }
    }

    /**
     * Boots one fresh in-memory database for a single test.
     *
     * @return the connected database manager, with its schema initialized.
     * @see [DatabaseManager]
     */
    private fun freshDatabase(): DatabaseManager =
        DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1").apply {
            databaseWrapper.connect()
            databaseWrapper.initializeSchema()
        }

    /**
     * Builds one harvested anime exactly as a streaming platform produces it.
     *
     * @param localizationTitle title carried by the harvested localization.
     * @param id identifier of the anime on the streaming platform.
     * @return the normalized harvest to persist.
     * @see [PlatformAnimeMockKBuilder]
     */
    private fun harvestedAnime(
        localizationTitle: String = "Title",
        id: String = "123"
    ): PlatformAnime =
        PlatformAnimeMockKBuilder {
            this.id = id
            platform = Platform.ANIMATION_DIGITAL_NETWORK
            normalizedTitle = canonicalTitle(mapOf(Country.FR.defaultLocale to localizationTitle))
            images = listOf(
                PlatformImageMockKBuilder {
                    type = ImageType.THUMBNAIL
                    url = "https://cdn.example/thumbnail.jpg"
                }.build()
            )
            localizations = listOf(
                PlatformAnimeLocalizationMockKBuilder {
                    country = Country.FR
                    locale = Country.FR.defaultLocale
                    title = localizationTitle
                    description = "Description"
                }.build()
            )
        }.build()

    /**
     * Builds one harvested episode carrying the given anime, with every field stubbed.
     *
     * The ingestion derives the content keys of the harvest from all these fields: an unstubbed
     * mock value would make two runs look like two different harvests.
     *
     * @param anime harvested anime carried by the episode.
     * @param id identifier of the episode on the streaming platform.
     * @return the harvested episode to persist.
     * @see [PlatformEpisodeMockKBuilder]
     */
    private fun harvestedEpisode(anime: PlatformAnime, id: String): PlatformEpisode =
        PlatformEpisodeMockKBuilder {
            this.anime = anime
            this.id = id
            image = "https://cdn.example/episode.jpg"
            releaseDateTime = ZonedDateTime.parse("2026-10-20T10:00:00Z")
            season = 1
            number = 1
            episodeType = EpisodeType.EPISODE
            duration = 1440
            url = "https://cdn.example/video"
            audioLocales = listOf(Locale.JA_JP)
            uncensored = false
            localizations = listOf(
                PlatformEpisodeLocalizationMockKBuilder {
                    country = Country.FR
                    title = "Episode 1"
                    description = "Description"
                }.build()
            )
        }.build()
}
