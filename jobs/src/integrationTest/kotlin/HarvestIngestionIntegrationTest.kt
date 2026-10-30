package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.framework.koin.applyTransactionalProxies
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.jobs.builders.harvest
import fr.shikkanime.jobs.builders.harvestedAnime
import fr.shikkanime.jobs.builders.harvestedEpisode
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeLocalizationMockKBuilder
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
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
@DisplayName("integration tests for the harvest ingestion")
class HarvestIngestionIntegrationTest {

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
            val saved = koin.get<HarvestIngestionService>()
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
        @DisplayName("should persist the episodes of a harvested anime through the real application bootstrap")
        fun shouldPersistTheEpisodesOfAHarvestedAnimeThroughTheRealApplicationBootstrap() {
            // Given
            freshDatabase()

            // When
            koin.get<HarvestIngestionService>()
                .saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime(id = "123"), id = "3001"))))

            // Then
            val anime = assertNotNull(
                koin.get<AnimeService>()
                    .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            )
            val episode = assertNotNull(
                koin.get<EpisodeService>()
                    .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "3001")
            )

            assertEquals(anime.anime.id, episode.episode.animeId)
            assertEquals(EpisodeType.EPISODE, episode.episode.episodeType)
            assertEquals("Episode", episode.localizations.single().title)
            assertEquals("https://example.test/episode.png", episode.images.single().url)
        }

        @Test
        @DisplayName("should update a stored episode when the same platform release is harvested again")
        fun shouldUpdateAStoredEpisodeWhenTheSamePlatformReleaseIsHarvestedAgain() {
            // Given: the same platform release is harvested twice
            freshDatabase()
            val ingestion = koin.get<HarvestIngestionService>()
            val harvest = harvest(true, harvestedEpisode(harvestedAnime(id = "123"), id = "3001"))

            // When
            ingestion.saveAll(listOf(harvest))
            ingestion.saveAll(listOf(harvest))

            // Then: the episode is updated, never duplicated
            val episode = assertNotNull(
                koin.get<EpisodeService>()
                    .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "3001")
            )
            val episodes = koin.get<EpisodeService>()
                .findAllByAnimeId(requireNotNull(episode.episode.animeId))

            assertEquals(1, episodes.size)
            assertEquals("Episode", episodes.single().localizations.single().title)
        }

        @Test
        @DisplayName("should persist the episode of a lower priority source without overwriting the stored text")
        fun shouldPersistTheEpisodeOfALowerPrioritySourceWithoutOverwritingTheStoredText() {
            // Given: the anime and its episode are stored from the highest priority source
            freshDatabase()
            koin.get<HarvestIngestionService>()
                .saveAll(listOf(harvest(true, harvestedEpisode(harvestedAnime(id = "123"), id = "3001"))))

            // When: a lower priority source publishes the same episode with its own text
            koin.get<HarvestIngestionService>()
                .saveAll(
                    listOf(
                        harvest(
                            true,
                            harvestedEpisode(
                                harvestedAnime(
                                    id = "456",
                                    platform = Platform.CRUNCHYROLL,
                                    image = "https://example.test/crunchyroll-banner.png"
                                ),
                                id = "4001",
                                image = "https://example.test/crunchyroll-episode.png",
                                localizations = listOf(
                                    PlatformEpisodeLocalizationMockKBuilder {
                                        country = Country.FR
                                        title = "Crunchyroll episode"
                                        description = "Crunchyroll description"
                                    }.build()
                                )
                            )
                        )
                    )
                )

            // Then: the anime keeps the ADN metadata and gains the link of the other source
            val anime = assertNotNull(
                koin.get<AnimeService>()
                    .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            )
            val otherSourceAnime = assertNotNull(
                koin.get<AnimeService>()
                    .findByPlatformAndPlatformId(Platform.CRUNCHYROLL, "456")
            )
            assertEquals(anime.anime.id, otherSourceAnime.anime.id)
            assertEquals("Title", otherSourceAnime.localizations.single().title)
            // The stored artwork is not deleted nor overwritten by the lower priority source
            assertEquals("https://example.test/banner.png", otherSourceAnime.images.single().url)
            val episode = assertNotNull(
                koin.get<EpisodeService>()
                    .findByPlatformAndPlatformId(Platform.CRUNCHYROLL, "4001")
            )

            assertEquals(anime.anime.id, episode.episode.animeId)
            assertEquals("Episode", episode.localizations.single().title)
            assertEquals("https://example.test/episode.png", episode.images.single().url)
            assertEquals(
                setOf(Platform.ANIMATION_DIGITAL_NETWORK, Platform.CRUNCHYROLL),
                episode.platforms.map { it.platform }.toSet()
            )
        }

        @Test
        @DisplayName("should take the episode metadata of the best source when it arrives after a lower priority one")
        fun shouldTakeTheEpisodeMetadataOfTheBestSourceWhenItArrivesAfterALowerPriorityOne() {
            // Given: a lower priority source is stored first
            freshDatabase()
            koin.get<HarvestIngestionService>()
                .saveAll(
                    listOf(
                        harvest(
                            true,
                            harvestedEpisode(
                                harvestedAnime(
                                    id = "456",
                                    platform = Platform.CRUNCHYROLL,
                                    image = "https://example.test/crunchyroll-banner.png"
                                ),
                                id = "4001",
                                image = "https://example.test/crunchyroll-episode.png",
                                duration = 1200
                            )
                        )
                    )
                )

            // When: the highest priority source publishes the same episode
            koin.get<HarvestIngestionService>()
                .saveAll(
                    listOf(
                        harvest(
                            true,
                            harvestedEpisode(harvestedAnime(id = "123"), id = "3001", duration = 1440)
                        )
                    )
                )

            // Then: the episode carries both releases and the metadata of ADN
            val episode = assertNotNull(
                koin.get<EpisodeService>()
                    .findByPlatformAndPlatformId(Platform.CRUNCHYROLL, "4001")
            )

            assertEquals(1440, episode.episode.duration)
            assertEquals("https://example.test/episode.png", episode.images.single().url)
            assertEquals(
                setOf(Platform.ANIMATION_DIGITAL_NETWORK, Platform.CRUNCHYROLL),
                episode.platforms.map { it.platform }.toSet()
            )
            val anime = assertNotNull(
                koin.get<AnimeService>()
                    .findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            )
            assertEquals(
                setOf(Platform.ANIMATION_DIGITAL_NETWORK, Platform.CRUNCHYROLL),
                anime.platforms.map { it.platform }.toSet()
            )
            assertEquals("https://example.test/banner.png", anime.images.single().url)
        }

        @Test
        @DisplayName("should update the stored anime when the same platform link is harvested again")
        fun shouldUpdateTheStoredAnimeWhenTheSamePlatformLinkIsHarvestedAgain() {
            // Given: the platform link is already stored from a previous harvest
            freshDatabase()
            koin.get<AnimeService>()
                .save(harvestedAnime(id = "456").toModel())
            val ingestion = koin.get<HarvestIngestionService>()

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
            val ingestion = koin.get<HarvestIngestionService>()

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
