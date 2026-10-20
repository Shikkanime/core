package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.savedAnime
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.Locale
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime localization repository")
class AnimeLocalizationRepositoryImplTest {
    @MockK private lateinit var animeRepository: AnimeRepository

    @InjectMockKs private lateinit var localizationRepository: AnimeLocalizationRepositoryImpl

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()

        every { animeRepository.getReference(any()) } answers { EntityID(firstArg(), AnimeTable) }
    }

    @Nested
    @DisplayName("tests for the 'findAllByAnimeIds' method")
    inner class FindAllByAnimeIdsTests {
        @Test
        @DisplayName("should load the localizations of every given anime in one grouped read")
        fun `should load the localizations of every given anime in one grouped read`() {
            // Given
            val anime = savedAnime()
            transaction {
                localizationRepository.save(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        locale = Locale.FR_FR
                        title = "One Piece"
                        description = "Description"
                    }.build()
                )
            }

            // When
            val found = transaction {
                localizationRepository.findAllByAnimeIds(listOf(requireNotNull(anime.id)))
            }

            // Then
            assertEquals("One Piece", found[anime.id]?.single()?.title)
        }
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should insert a localization for a locale not translated yet")
        fun `should insert a localization for a locale not translated yet`() {
            // Given
            val anime = savedAnime()
            val localization = AnimeLocalizationMockKBuilder {
                id = Uuid.random()
                animeId = anime.id
                locale = Locale.FR_FR
                title = "One Piece"
                description = "Description"
            }.build()

            // When
            val saved = transaction { localizationRepository.save(localization) }

            // Then
            val found = assertNotNull(
                transaction { localizationRepository.findModelById(requireNotNull(saved.id)) }
            )
            assertEquals(Locale.FR_FR, found.locale)
            assertEquals("One Piece", found.title)
        }

        @Test
        @DisplayName("should refresh the title and description in place for a known locale")
        fun `should refresh the title and description in place for a known locale`() {
            // Given
            val anime = savedAnime()
            val first = transaction {
                localizationRepository.save(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        locale = Locale.FR_FR
                        title = "One"
                        description = "Description"
                    }.build()
                )
            }
            val changed = AnimeLocalizationMockKBuilder {
                id = Uuid.random()
                animeId = anime.id
                locale = Locale.FR_FR
                title = "One Renamed"
                description = "Synopsys"
            }.build()

            // When
            val saved = transaction { localizationRepository.save(changed) }

            // Then
            assertEquals(first.id, saved.id)
            assertEquals("One Renamed", saved.title)
            assertEquals("Synopsys", saved.description)
        }

        @Test
        @DisplayName("should append a new locale without removing the existing ones")
        fun `should append a new locale without removing the existing ones`() {
            // Given
            val anime = savedAnime()
            val first = transaction {
                localizationRepository.save(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        locale = Locale.FR_FR
                        title = "One"
                        description = "Description"
                    }.build()
                )
            }

            // When
            val second = transaction {
                localizationRepository.save(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        locale = Locale.DE_DE
                        title = "One DE"
                        description = "Description DE"
                    }.build()
                )
            }

            // Then
            assertNotNull(transaction { localizationRepository.findModelById(requireNotNull(first.id)) })
            assertNotNull(transaction { localizationRepository.findModelById(requireNotNull(second.id)) })
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
            val anime = savedAnime()
            val saved = transaction {
                localizationRepository.save(
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        locale = Locale.FR_FR
                        title = "One Piece"
                        description = "Description"
                    }.build()
                )
            }

            // When
            val found = assertNotNull(
                transaction { localizationRepository.findModelById(requireNotNull(saved.id)) }
            )

            // Then
            assertEquals(anime.id, found.animeId)
            assertEquals("One Piece", found.title)
        }
    }
}
