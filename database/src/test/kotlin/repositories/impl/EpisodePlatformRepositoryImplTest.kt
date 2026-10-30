package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.EpisodePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.savedEpisode
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@DisplayName("tests for the episode platform repository")
class EpisodePlatformRepositoryImplTest {
    private val platformRepository = EpisodePlatformRepositoryImpl()

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()
    }

    @Nested
    @DisplayName("tests for the 'findAllByEpisodeIds' method")
    inner class FindAllByEpisodeIdsTests {
        @Test
        @DisplayName("should load the platform releases of every given episode in one grouped read")
        fun `should load the platform releases of every given episode in one grouped read`() {
            // Given
            val episode = savedEpisode()
            transaction {
                platformRepository.save(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // When
            val found = transaction {
                platformRepository.findAllByEpisodeIds(listOf(requireNotNull(episode.id)))
            }

            // Then
            assertEquals("3001", found[episode.id]?.single()?.platformId)
        }
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should insert a release for a new platform identifier")
        fun `should insert a release for a new platform identifier`() {
            // Given
            val episode = savedEpisode()
            val release = EpisodePlatformMockKBuilder {
                id = Uuid.random()
                episodeId = episode.id
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001"
                audioLocales = setOf(Locale.FR_FR, Locale.JA_JP)
                uncensored = true
                releaseDateTime = TimeUtils.now()
            }.build()

            // When
            val saved = transaction { platformRepository.save(release) }

            // Then
            val found = assertNotNull(
                transaction { platformRepository.findModelById(requireNotNull(saved.id)) }
            )
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, found.platform)
            assertEquals("3001", found.platformId)
            assertEquals(setOf(Locale.FR_FR, Locale.JA_JP), found.audioLocales)
            assertEquals(true, found.uncensored)
        }

        @Test
        @DisplayName("should refresh the release in place when the same platform identifier is saved twice")
        fun `should refresh the release in place when the same platform identifier is saved twice`() {
            // Given
            val episode = savedEpisode()
            val first = transaction {
                platformRepository.save(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }
            val changed = EpisodePlatformMockKBuilder {
                id = Uuid.random()
                episodeId = episode.id
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001-uncensored"
                audioLocales = setOf(Locale.FR_FR, Locale.JA_JP)
                uncensored = true
                releaseDateTime = TimeUtils.now()
            }.build()

            // When
            val saved = transaction { platformRepository.save(changed) }

            // Then
            assertEquals(first.id, saved.id)
            assertEquals("https://example.test/episodes/3001-uncensored", saved.url)
            assertEquals(setOf(Locale.FR_FR, Locale.JA_JP), saved.audioLocales)
            assertEquals(true, saved.uncensored)
        }

        @Test
        @DisplayName("should keep releases of different platform identifiers side by side")
        fun `should keep releases of different platform identifiers side by side`() {
            // Given
            val episode = savedEpisode()
            val first = transaction {
                platformRepository.save(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // When
            val second = transaction {
                platformRepository.save(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3002"
                        url = "https://example.test/episodes/3002"
                        audioLocales = setOf(Locale.JA_JP)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // Then
            assertNotNull(transaction { platformRepository.findModelById(requireNotNull(first.id)) })
            assertNotNull(transaction { platformRepository.findModelById(requireNotNull(second.id)) })
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        @DisplayName("should return null when the platform release does not exist")
        fun `should return null when the platform release does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { platformRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the stored platform release")
        fun `should return the stored platform release`() {
            // Given
            val episode = savedEpisode()
            val saved = transaction {
                platformRepository.save(
                    EpisodePlatformMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocales = setOf(Locale.FR_FR)
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // When
            val found = assertNotNull(
                transaction { platformRepository.findModelById(requireNotNull(saved.id)) }
            )

            // Then
            assertEquals(episode.id, found.episodeId)
            assertEquals("3001", found.platformId)
        }
    }
}
