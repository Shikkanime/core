package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.models.ImageType
import io.mockk.impl.annotations.InjectMockKs
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
@DisplayName("tests for the image repository")
class ImageRepositoryImplTest {
    @InjectMockKs private lateinit var imageRepository: ImageRepositoryImpl

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()
    }

    @Nested
    @DisplayName("tests for the 'findAllByEntityId' method")
    inner class FindAllByEntityIdTests {
        @Test
        fun `should return an empty list when the owner has no image`() {
            // Given
            val owner = Uuid.random()

            // When
            val found = transaction { imageRepository.findAllByEntityId(owner) }

            // Then
            assertTrue(found.isEmpty())
        }

        @Test
        fun `should return only the images of the given owner`() {
            // Given
            val owner = Uuid.random()
            val other = Uuid.random()
            transaction {
                imageRepository.save(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        type = ImageType.THUMBNAIL
                        entityId = owner
                    }.build()
                )
                imageRepository.save(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        type = ImageType.BANNER
                        entityId = other
                    }.build()
                )
            }

            // When
            val found = transaction { imageRepository.findAllByEntityId(owner) }

            // Then
            assertEquals(ImageType.THUMBNAIL, found.single().type)
        }
    }

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        fun `should insert a new image for a type not set yet`() {
            // Given
            val image = ImageMockKBuilder {
                id = Uuid.random()
                type = ImageType.BANNER
                url = "https://a"
            }.build()

            // When
            val saved = transaction { imageRepository.save(image) }

            // Then
            val found = assertNotNull(transaction { imageRepository.findModelById(requireNotNull(saved.id)) })
            assertEquals("https://a", found.url)
        }

        @Test
        fun `should overwrite the url in place for a known type`() {
            // Given
            val owner = Uuid.random()
            val image = ImageMockKBuilder {
                id = Uuid.random()
                entityId = owner
                type = ImageType.BANNER
                url = "https://a"
            }.build()
            val first = transaction { imageRepository.save(image) }
            val changed = ImageMockKBuilder {
                id = Uuid.random()
                entityId = owner
                type = ImageType.BANNER
                url = "https://b"
            }.build()

            // When
            val saved = transaction { imageRepository.save(changed) }

            // Then
            assertEquals(first.id, saved.id)
            assertEquals("https://b", saved.url)
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        fun `should return null when the image does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { imageRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        fun `should return the stored image`() {
            // Given
            val saved = transaction {
                imageRepository.save(
                    ImageMockKBuilder {
                        id = Uuid.random()
                        type = ImageType.THUMBNAIL
                        url = "https://a"
                    }.build()
                )
            }

            // When
            val found = assertNotNull(transaction { imageRepository.findModelById(requireNotNull(saved.id)) })

            // Then
            assertEquals(ImageType.THUMBNAIL, found.type)
        }
    }
}
