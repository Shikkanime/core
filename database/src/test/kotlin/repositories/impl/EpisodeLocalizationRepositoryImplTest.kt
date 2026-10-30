package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.savedEpisode
import fr.shikkanime.models.Locale
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@DisplayName("tests for the episode localization repository")
class EpisodeLocalizationRepositoryImplTest {
    private val localizationRepository = EpisodeLocalizationRepositoryImpl()

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
        @DisplayName("should load the localizations of every given episode in one grouped read")
        fun `should load the localizations of every given episode in one grouped read`() {
            // Given
            val episode = savedEpisode()
            transaction {
                localizationRepository.save(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        locale = Locale.FR_FR
                        title = "Episode"
                        description = "Description"
                    }.build()
                )
            }

            // When
            val found = transaction {
                localizationRepository.findAllByEpisodeIds(listOf(requireNotNull(episode.id)))
            }

            // Then
            assertEquals("Episode", found[episode.id]?.single()?.title)
        }
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should insert a localization for a locale not translated yet")
        fun `should insert a localization for a locale not translated yet`() {
            // Given
            val episode = savedEpisode()
            val localization = EpisodeLocalizationMockKBuilder {
                id = Uuid.random()
                episodeId = episode.id
                locale = Locale.FR_FR
                title = "Episode"
                description = "Description"
            }.build()

            // When
            val saved = transaction { localizationRepository.save(localization) }

            // Then
            val found = assertNotNull(
                transaction { localizationRepository.findModelById(requireNotNull(saved.id)) }
            )
            assertEquals(Locale.FR_FR, found.locale)
            assertEquals("Episode", found.title)
        }

        @Test
        @DisplayName("should refresh the title and description in place for a known locale")
        fun `should refresh the title and description in place for a known locale`() {
            // Given
            val episode = savedEpisode()
            val first = transaction {
                localizationRepository.save(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        locale = Locale.FR_FR
                        title = "Episode"
                        description = "Description"
                    }.build()
                )
            }
            val changed = EpisodeLocalizationMockKBuilder {
                id = Uuid.random()
                episodeId = episode.id
                locale = Locale.FR_FR
                title = "Episode renamed"
                description = "Synopsis"
            }.build()

            // When
            val saved = transaction { localizationRepository.save(changed) }

            // Then
            assertEquals(first.id, saved.id)
            assertEquals("Episode renamed", saved.title)
            assertEquals("Synopsis", saved.description)
        }

        @Test
        @DisplayName("should append a new locale without removing the existing ones")
        fun `should append a new locale without removing the existing ones`() {
            // Given
            val episode = savedEpisode()
            transaction {
                localizationRepository.save(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        locale = Locale.FR_FR
                        title = "Episode"
                        description = "Description"
                    }.build()
                )
            }

            // When
            val second = transaction {
                localizationRepository.save(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        locale = Locale.DE_DE
                        title = "Episode DE"
                        description = "Beschreibung"
                    }.build()
                )
            }

            // Then
            assertNotNull(transaction { localizationRepository.findModelById(requireNotNull(second.id)) })
            assertEquals(
                2,
                transaction { localizationRepository.findAllByEpisodeIds(listOf(requireNotNull(episode.id))) }
                    .values
                    .single()
                    .size
            )
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        @DisplayName("should return null when the localization does not exist")
        fun `should return null when the localization does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { localizationRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the stored localization")
        fun `should return the stored localization`() {
            // Given
            val episode = savedEpisode()
            val saved = transaction {
                localizationRepository.save(
                    EpisodeLocalizationMockKBuilder {
                        id = Uuid.random()
                        episodeId = episode.id
                        locale = Locale.FR_FR
                        title = "Episode"
                        description = "Description"
                    }.build()
                )
            }

            // When
            val found = assertNotNull(
                transaction { localizationRepository.findModelById(requireNotNull(saved.id)) }
            )

            // Then
            assertEquals(episode.id, found.episodeId)
            assertEquals("Episode", found.title)
        }
    }
}
