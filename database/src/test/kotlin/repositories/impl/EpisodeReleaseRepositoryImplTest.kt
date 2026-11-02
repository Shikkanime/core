package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.EpisodeReleaseMockKBuilder
import fr.shikkanime.database.builders.impl.savedEpisode
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@DisplayName("tests for the episode release repository")
class EpisodeReleaseRepositoryImplTest {
    private val releaseRepository = EpisodeReleaseRepositoryImpl()

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
        @DisplayName("should load the releases of every given episode in one grouped read")
        fun `should load the releases of every given episode in one grouped read`() {
            // Given
            val episode = savedEpisode()
            transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // When
            val found = transaction {
                releaseRepository.findAllByEpisodeIds(listOf(requireNotNull(episode.id)))
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
            val release = EpisodeReleaseMockKBuilder {
                id = Uuid.random()
                episodeId = episode.id
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001"
                audioLocale = Locale.FR_FR
                uncensored = true
                releaseDateTime = TimeUtils.now()
            }.build()

            // When
            val saved = transaction { releaseRepository.save(release) }

            // Then
            val found = assertNotNull(
                transaction { releaseRepository.findModelById(requireNotNull(saved.id)) }
            )
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, found.platform)
            assertEquals("3001", found.platformId)
            assertEquals(Locale.FR_FR, found.audioLocale)
            assertEquals(true, found.uncensored)
        }

        @Test
        @DisplayName("should refresh the release in place when the same video and locale is saved twice")
        fun `should refresh the release in place when the same video and locale is saved twice`() {
            // Given
            val episode = savedEpisode()
            val first = transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }
            val changed = EpisodeReleaseMockKBuilder {
                id = Uuid.random()
                episodeId = episode.id
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001-uncensored"
                audioLocale = Locale.FR_FR
                uncensored = true
                releaseDateTime = TimeUtils.now()
            }.build()

            // When
            val saved = transaction { releaseRepository.save(changed) }

            // Then
            assertEquals(first.id, saved.id)
            assertEquals("https://example.test/episodes/3001-uncensored", saved.url)
            assertEquals(true, saved.uncensored)
        }

        @Test
        @DisplayName("should keep the audio tracks of one video side by side, each with its own date")
        fun `should keep the audio tracks of one video side by side, each with its own date`() {
            // Given: the same video publishes a subtitled and a dubbed release on different dates
            val episode = savedEpisode()
            val subtitledDate = LocalDateTime(2026, 1, 1, 12, 0)
            val dubbedDate = LocalDateTime(2026, 1, 8, 12, 0)
            val subtitled = transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.JA_JP
                        uncensored = false
                        releaseDateTime = subtitledDate
                    }.build()
                )
            }

            // When
            val dubbed = transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001-vf"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = dubbedDate
                    }.build()
                )
            }

            // Then: two rows of the same video, each one playing its own locale on its own date
            assertNotNull(transaction { releaseRepository.findModelById(requireNotNull(subtitled.id)) })
            assertNotNull(transaction { releaseRepository.findModelById(requireNotNull(dubbed.id)) })
            assertEquals(Locale.JA_JP, subtitled.audioLocale)
            assertEquals(subtitledDate, subtitled.releaseDateTime)
            assertEquals(Locale.FR_FR, dubbed.audioLocale)
            assertEquals(dubbedDate, dubbed.releaseDateTime)
        }

        @Test
        @DisplayName("should keep releases of different platform identifiers side by side")
        fun `should keep releases of different platform identifiers side by side`() {
            // Given
            val episode = savedEpisode()
            val first = transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // When
            val second = transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3002"
                        url = "https://example.test/episodes/3002"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // Then
            assertNotNull(transaction { releaseRepository.findModelById(requireNotNull(first.id)) })
            assertNotNull(transaction { releaseRepository.findModelById(requireNotNull(second.id)) })
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        @DisplayName("should return null when the release does not exist")
        fun `should return null when the release does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { releaseRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the stored release")
        fun `should return the stored release`() {
            // Given
            val episode = savedEpisode()
            val saved = transaction {
                releaseRepository.save(
                    EpisodeReleaseMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "3001"
                        url = "https://example.test/episodes/3001"
                        audioLocale = Locale.FR_FR
                        uncensored = false
                        releaseDateTime = TimeUtils.now()
                    }.build()
                )
            }

            // When
            val found = assertNotNull(
                transaction { releaseRepository.findModelById(requireNotNull(saved.id)) }
            )

            // Then
            assertEquals(episode.id, found.episodeId)
            assertEquals("3001", found.platformId)
            assertEquals(Locale.FR_FR, found.audioLocale)
        }
    }
}
