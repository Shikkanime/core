package fr.shikkanime.database.services

import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.impl.ImageServiceImpl
import fr.shikkanime.models.ImageType
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the image service")
class ImageServiceTest {
    @MockK private lateinit var imageRepository: ImageRepository
    @InjectMockKs private lateinit var imageService: ImageServiceImpl

    @Nested
    @DisplayName("tests for the 'findByEntityIdAndType' method")
    inner class FindByEntityIdAndTypeTests {
        @Test
        @DisplayName("should return the stored image")
        fun `should return the stored image`() {
            // Given
            val ownerId = Uuid.random()
            val image = ImageMockKBuilder {
                id = Uuid.random()
                entityId = ownerId
                type = ImageType.THUMBNAIL
                url = "https://example.test/thumbnail.png"
            }.build()
            every {
                imageRepository.findByEntityIdAndType(ownerId, ImageType.THUMBNAIL)
            } returns image

            // When
            val found = imageService.findByEntityIdAndType(ownerId, ImageType.THUMBNAIL)

            // Then
            assertEquals(image, found)
        }

        @Test
        @DisplayName("should return null when the image is not stored")
        fun `should return null when the image is not stored`() {
            // Given
            val entityId = Uuid.random()
            every {
                imageRepository.findByEntityIdAndType(entityId, ImageType.BANNER)
            } returns null

            // When
            val found = imageService.findByEntityIdAndType(entityId, ImageType.BANNER)

            // Then
            assertNull(found)
        }
    }
}
