package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.savedAnime
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.Platform
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
@DisplayName("tests for the anime platform repository")
class AnimePlatformRepositoryImplTest {
    @MockK private lateinit var animeRepository: AnimeRepository

    @InjectMockKs private lateinit var platformRepository: AnimePlatformRepositoryImpl

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
        @DisplayName("should load the platform links of every given anime in one grouped read")
        fun `should load the platform links of every given anime in one grouped read`() {
            // Given
            val anime = savedAnime()
            transaction {
                platformRepository.save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // When
            val found = transaction {
                platformRepository.findAllByAnimeIds(listOf(requireNotNull(anime.id)))
            }

            // Then
            assertEquals("123", found[anime.id]?.single()?.platformId)
        }
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should insert a link for a new platform identifier")
        fun `should insert a link for a new platform identifier`() {
            // Given
            val anime = savedAnime()
            val link = AnimePlatformMockKBuilder {
                id = Uuid.random()
                animeId = anime.id
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "123"
            }.build()

            // When
            val saved = transaction { platformRepository.save(link) }

            // Then
            val found = assertNotNull(
                transaction { platformRepository.findModelById(requireNotNull(saved.id)) }
            )
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, found.platform)
            assertEquals("123", found.platformId)
        }

        @Test
        @DisplayName("should keep the same row when the platform link is saved twice")
        fun `should keep the same row when the platform link is saved twice`() {
            // Given
            val anime = savedAnime()
            val first = transaction {
                platformRepository.save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // When
            val second = transaction {
                platformRepository.save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // Then
            assertEquals(first.id, second.id)
        }

        @Test
        @DisplayName("should keep links of different platform identifiers side by side")
        fun `should keep links of different platform identifiers side by side`() {
            // Given
            val anime = savedAnime()
            val first = transaction {
                platformRepository.save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // When
            val second = transaction {
                platformRepository.save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "456"
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
        @DisplayName("should return null when the platform link does not exist")
        fun `should return null when the platform link does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { platformRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the stored platform link")
        fun `should return the stored platform link`() {
            // Given
            val anime = savedAnime()
            val saved = transaction {
                platformRepository.save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // When
            val found = assertNotNull(
                transaction { platformRepository.findModelById(requireNotNull(saved.id)) }
            )

            // Then
            assertEquals(anime.id, found.animeId)
            assertEquals("123", found.platformId)
        }
    }
}
