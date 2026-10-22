package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.framework.koin.applyTransactionalProxies
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.builders.harvest
import fr.shikkanime.jobs.builders.harvestedAnime
import fr.shikkanime.jobs.builders.harvestedEpisode
import fr.shikkanime.models.Platform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.koin.plugin.module.dsl.startKoin
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
                .saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime(id = "123"), id = "3001"))))

            // Then
            assertEquals(1, saved)
            val found = koin.get<AnimeService>()
                .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")

            assertNotNull(found)
            assertEquals("Title", found.localizations.single().title)
            assertEquals("https://example.test/banner.png", found.images.single().url)
        }

        @Test
        @DisplayName("should update the stored anime when the same platform link is harvested again")
        fun shouldUpdateTheStoredAnimeWhenTheSamePlatformLinkIsHarvestedAgain() {
            // Given: the platform link is already stored from a previous harvest
            freshDatabase()
            koin.get<AnimeService>()
                .save(harvestedAnime(id = "456").toModel())
            val ingestion = koin.get<AnimeIngestionService>()

            // When
            ingestion.saveAll(
                listOf(
                    harvest(
                        true,
                        harvestedEpisode(
                            harvestedAnime(title = "Title (2024)", id = "456"),
                            id = "5001"
                        )
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
            // Given: the platform link is already stored under its identity name
            freshDatabase()
            koin.get<AnimeService>()
                .save(harvestedAnime(id = "789").toModel())
            val ingestion = koin.get<AnimeIngestionService>()

            // When
            ingestion.saveAll(
                listOf(
                    harvest(
                        true,
                        harvestedEpisode(
                            harvestedAnime(title = "Renamed", id = "789"),
                            id = "7001"
                        )
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
}
