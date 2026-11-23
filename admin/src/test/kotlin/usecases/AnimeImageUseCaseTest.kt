package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.usecases.impl.AnimeImageUseCaseImpl
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.models.ImageType
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for AnimeImageUseCase")
class AnimeImageUseCaseTest {
    @MockK private lateinit var animeService: AnimeService
    @InjectMockKs private lateinit var animeImageUseCase: AnimeImageUseCaseImpl

    @Nested
    @DisplayName("tests for the 'update' method")
    inner class UpdateTests {
        @Test
        @DisplayName("should return the overridden image when the anime exists")
        fun `should return the overridden image when the anime exists`() {
            // Given
            val animeId = Uuid.random()
            val stored = ImageMockKBuilder {
                entityId = animeId
                type = ImageType.THUMBNAIL
                url = "https://manual"
                manual = true
            }.build()
            every { animeService.saveManualImage(animeId, ImageType.THUMBNAIL, "https://manual") } returns stored

            // When
            val result = animeImageUseCase.update(animeId, ImageType.THUMBNAIL, "https://manual")

            // Then
            assertEquals(ImageType.THUMBNAIL, result?.type)
            assertEquals("https://manual", result?.url)
            assertTrue(result?.manual == true)
        }

        @Test
        @DisplayName("should trim the url before persisting it")
        fun `should trim the url before persisting it`() {
            // Given
            val animeId = Uuid.random()
            val stored = ImageMockKBuilder {
                entityId = animeId
                type = ImageType.BANNER
                url = "https://a"
                manual = true
            }.build()
            every { animeService.saveManualImage(animeId, ImageType.BANNER, "https://a") } returns stored

            // When
            animeImageUseCase.update(animeId, ImageType.BANNER, "  https://a  ")

            // Then
            verify(exactly = 1) { animeService.saveManualImage(animeId, ImageType.BANNER, "https://a") }
        }

        @Test
        @DisplayName("should return null when the anime does not exist")
        fun `should return null when the anime does not exist`() {
            // Given
            val unknown = Uuid.random()
            every { animeService.saveManualImage(unknown, ImageType.TITLE, "https://a") } returns null

            // When
            val result = animeImageUseCase.update(unknown, ImageType.TITLE, "https://a")

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("should reject a url that is not http or https")
        fun `should reject a url that is not http or https`() {
            // Given
            val animeId = Uuid.random()

            // When & Then
            assertThrows<IllegalArgumentException> {
                animeImageUseCase.update(animeId, ImageType.THUMBNAIL, "javascript:alert(1)")
            }
            verify(exactly = 0) { animeService.saveManualImage(any(), any(), any()) }
        }

        @Test
        @DisplayName("should reject a url longer than the column length")
        fun `should reject a url longer than the column length`() {
            // Given
            val animeId = Uuid.random()
            val tooLong = "https://a/" + "b".repeat(1000)

            // When & Then
            assertThrows<IllegalArgumentException> {
                animeImageUseCase.update(animeId, ImageType.THUMBNAIL, tooLong)
            }
            verify(exactly = 0) { animeService.saveManualImage(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("tests for the 'reset' method")
    inner class ResetTests {
        @Test
        @DisplayName("should return the released image")
        fun `should return the released image`() {
            // Given
            val animeId = Uuid.random()
            val released = ImageMockKBuilder {
                entityId = animeId
                type = ImageType.THUMBNAIL
                url = "https://a"
                manual = false
            }.build()
            every { animeService.clearManualImage(animeId, ImageType.THUMBNAIL) } returns released

            // When
            val result = animeImageUseCase.reset(animeId, ImageType.THUMBNAIL)

            // Then
            assertEquals("https://a", result?.url)
            assertEquals(false, result?.manual)
        }

        @Test
        @DisplayName("should return null when the anime has no image of this type")
        fun `should return null when the anime has no image of this type`() {
            // Given
            val animeId = Uuid.random()
            every { animeService.clearManualImage(animeId, ImageType.TITLE) } returns null

            // When
            val result = animeImageUseCase.reset(animeId, ImageType.TITLE)

            // Then
            assertNull(result)
        }
    }
}
