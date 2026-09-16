package fr.shikkanime.database.services

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.database.repositories.impl.EpisodeRepositoryImpl
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.database.services.impl.EpisodeServiceImpl
import fr.shikkanime.exposed.DatabaseWrapper
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlinx.datetime.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("tests for EpisodeService")
class EpisodeServiceTest {

    private lateinit var episodeService: EpisodeService

    @BeforeAll
    fun setUp() {
        // Given a connected in-memory H2 database with the schema initialized
        val databaseManager = DatabaseManager()
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()

        // Wrap the service in a TransactionalProxy so @Transactional opens real transactions
        episodeService = TransactionalProxy(
            EpisodeServiceImpl(EpisodeRepositoryImpl()),
            EpisodeService::class.java
        ).create()
    }

    @AfterAll
    fun tearDown() {
        // Nothing to release: H2 in-memory dies with the JVM
    }

    @Nested
    @DisplayName("tests for the 'findAllExistingPlatformEpisodeIds' method")
    inner class FindAllExistingPlatformEpisodeIdsTests {

        @Test
        @DisplayName("should return an empty set when none of the platform episode ids exist")
        fun `should return an empty set when none of the platform episode ids exist`() {
            // Given
            val ids = listOf("9999-a", "9999-b")

            // When
            val existing = episodeService.findAllExistingPlatformEpisodeIds(
                Platform.ANIMATION_DIGITAL_NETWORK,
                ids
            )

            // Then
            assertTrue(existing.isEmpty())
        }

        @Test
        @DisplayName("should return only the ids that already exist for the given platform")
        fun `should return only the ids that already exist for the given platform`() {
            // Given
            episodeService.save(
                platform = Platform.ANIMATION_DIGITAL_NETWORK,
                platformEpisodeId = "existing-1",
                title = "Episode 1",
                releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
            )
            episodeService.save(
                platform = Platform.CRUNCHYROLL,
                platformEpisodeId = "existing-2",
                title = "Episode 2",
                releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
            )

            // When
            val existing = episodeService.findAllExistingPlatformEpisodeIds(
                Platform.ANIMATION_DIGITAL_NETWORK,
                listOf("existing-1", "existing-2", "missing")
            )

            // Then
            assertEquals(setOf("existing-1"), existing)
        }
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {

        @Test
        @DisplayName("should save a new episode and return it")
        fun `should save a new episode and return it`() {
            // When
            val saved = episodeService.save(
                platform = Platform.ANIMATION_DIGITAL_NETWORK,
                platformEpisodeId = "save-new-1",
                title = "Major - Episode 105",
                releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
            )

            // Then
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, saved.platform)
            assertEquals("save-new-1", saved.platformEpisodeId)
            assertEquals("Major - Episode 105", saved.title)
            assertEquals(LocalDateTime(2026, 1, 10, 8, 0, 0), saved.releaseDateTime)
        }

        @Test
        @DisplayName("should not duplicate an episode with the same platform and platform episode id")
        fun `should not duplicate an episode with the same platform and platform episode id`() {
            // Given
            episodeService.save(
                platform = Platform.ANIMATION_DIGITAL_NETWORK,
                platformEpisodeId = "dedup-1",
                title = "Episode 1",
                releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
            )

            // When
            episodeService.save(
                platform = Platform.ANIMATION_DIGITAL_NETWORK,
                platformEpisodeId = "dedup-1",
                title = "Episode 1",
                releaseDateTime = LocalDateTime(2026, 1, 10, 8, 0, 0)
            )
            val existing = episodeService.findAllExistingPlatformEpisodeIds(
                Platform.ANIMATION_DIGITAL_NETWORK,
                listOf("dedup-1")
            )

            // Then
            assertEquals(setOf("dedup-1"), existing)
            assertEquals(1L, countEpisodes())
        }
    }

    private fun countEpisodes(): Long =
        transaction { EpisodeEntity.all().count() }
}
