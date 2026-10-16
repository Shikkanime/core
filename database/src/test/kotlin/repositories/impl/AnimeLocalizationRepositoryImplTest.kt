package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.Anime
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

    private fun savedAnime(): Anime {
        val anime = AnimeMockKBuilder { id = Uuid.random() }
            .build()

        transaction { AnimeEntity.new(anime.id) { } }

        return anime
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        fun `should insert a localization for a locale not translated yet`() {
            // Given
            val anime = savedAnime()
            val localization = AnimeLocalizationMockKBuilder {
                id = Uuid.random()
                locale = Locale.FR_FR
                title = "One Piece"
            }.build()

            // When
            val saved = transaction { localizationRepository.save(anime, localization) }

            // Then
            val found = assertNotNull(transaction { localizationRepository.findModelById(requireNotNull(saved.id)) })
            assertEquals(Locale.FR_FR, found.locale)
            assertEquals("One Piece", found.title)
        }

        @Test
        fun `should refresh the title and description in place for a known locale`() {
            // Given
            val anime = savedAnime()
            val first = transaction {
                localizationRepository.save(
                    anime,
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        locale = Locale.FR_FR
                        title = "One"
                    }.build()
                )
            }
            val changed = AnimeLocalizationMockKBuilder {
                id = Uuid.random()
                locale = Locale.FR_FR
                title = "One Renamed"
                description = "Synopsys"
            }.build()

            // When
            val saved = transaction { localizationRepository.save(anime, changed) }

            // Then
            assertEquals(first.id, saved.id)
            assertEquals("One Renamed", saved.title)
            assertEquals("Synopsys", saved.description)
        }

        @Test
        fun `should append a new locale without removing the existing ones`() {
            // Given
            val anime = savedAnime()
            val first = transaction {
                localizationRepository.save(
                    anime,
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        locale = Locale.FR_FR
                        title = "One"
                    }.build()
                )
            }

            // When
            val second = transaction {
                localizationRepository.save(
                    anime,
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        locale = Locale.DE_DE
                        title = "One DE"
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
        fun `should return null when the localization does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { localizationRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        fun `should return the stored localization`() {
            // Given
            val anime = savedAnime()
            val saved = transaction {
                localizationRepository.save(
                    anime,
                    AnimeLocalizationMockKBuilder {
                        id = Uuid.random()
                        locale = Locale.FR_FR
                        title = "One Piece"
                    }.build()
                )
            }

            // When
            val found = assertNotNull(transaction { localizationRepository.findModelById(requireNotNull(saved.id)) })

            // Then
            assertEquals("One Piece", found.title)
        }
    }
}
