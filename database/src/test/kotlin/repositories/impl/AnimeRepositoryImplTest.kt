package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime repository")
class AnimeRepositoryImplTest {
    @MockK private lateinit var imageRepository: ImageRepository

    @InjectMockKs private lateinit var animeRepository: AnimeRepositoryImpl

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()

        every { imageRepository.findAllByEntityId(any()) } returns emptyList()
        every { imageRepository.findAllByEntityIds(any()) } returns emptyMap()
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        fun `should create the anime identity row when it does not exist yet`() {
            // Given
            val anime = AnimeMockKBuilder { id = Uuid.random() }
                .build()

            // When
            val saved = transaction { animeRepository.save(anime) }

            // Then
            val found = transaction { animeRepository.findModelById(saved.id) }
            assertEquals(anime.id, assertNotNull(found).id)
        }

        @Test
        fun `should return the existing anime untouched when the same identifier is saved twice`() {
            // Given
            val anime = AnimeMockKBuilder { id = Uuid.random() }
                .build()
            val first = transaction { animeRepository.save(anime) }

            // When
            val second = transaction { animeRepository.save(anime) }

            // Then
            assertEquals(first.id, second.id)
            assertEquals(first.createdAt, second.createdAt)
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        fun `should return null when the anime does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { animeRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        fun `should return the anime with its images composed from their repository`() {
            // Given
            val anime = AnimeMockKBuilder { id = Uuid.random() }
                .build()
            val images = listOf(
                ImageMockKBuilder {
                    id = Uuid.random()
                    type = ImageType.BANNER
                    entityId = anime.id
                }.build()
            )
            every { imageRepository.findAllByEntityId(any()) } returns images
            transaction { animeRepository.save(anime) }

            // When
            val found = assertNotNull(transaction { animeRepository.findModelById(anime.id) })

            // Then
            assertEquals(ImageType.BANNER, found.images.single().type)
        }

        @Test
        fun `should return an anime without child when it has none`() {
            // Given
            val anime = AnimeMockKBuilder { id = Uuid.random() }
                .build()
            transaction { animeRepository.save(anime) }

            // When
            val found = assertNotNull(transaction { animeRepository.findModelById(anime.id) })

            // Then
            assertTrue(found.localizations.isEmpty())
            assertTrue(found.platforms.isEmpty())
            assertTrue(found.images.isEmpty())
        }
    }

    @Nested
    @DisplayName("tests for the 'findAllModels' method")
    inner class FindAllTests {
        @Test
        fun `should return an empty list when the database holds no anime`() {
            // Given
            // When
            val found = transaction { animeRepository.findAllModels() }

            // Then
            assertTrue(found.isEmpty())
        }

        @Test
        fun `should return every stored anime`() {
            // Given
            transaction {
                animeRepository.save(AnimeMockKBuilder { id = Uuid.random() }.build())
                animeRepository.save(AnimeMockKBuilder { id = Uuid.random() }.build())
            }

            // When
            val found = transaction { animeRepository.findAllModels() }

            // Then
            assertEquals(2, found.size)
        }
    }

    @Nested
    @DisplayName("tests for the 'findByPlatformAndPlatformId' method")
    inner class FindByPlatformAndPlatformIdTests {
        @Test
        fun `should return null when no anime carries the platform link`() {
            // Given
            // When
            val found = transaction {
                animeRepository.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "404")
            }

            // Then
            assertNull(found)
        }

        @Test
        fun `should return the anime owning the platform link`() {
            // Given
            val anime = AnimeMockKBuilder { id = Uuid.random() }
                .build()
            transaction {
                animeRepository.save(anime)
                AnimePlatformRepositoryImpl(animeRepository).save(
                    anime,
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // When
            val found = transaction {
                animeRepository.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            }

            // Then
            assertEquals(anime.id, assertNotNull(found).id)
        }
    }
}
