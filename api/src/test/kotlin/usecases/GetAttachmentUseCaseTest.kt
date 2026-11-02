package fr.shikkanime.api.usecases

import fr.shikkanime.api.images.WebpConversionException
import fr.shikkanime.api.usecases.impl.GetAttachmentUseCaseImpl
import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.services.ImageService
import fr.shikkanime.models.ImageType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the attachment use case")
class GetAttachmentUseCaseTest {
    @MockK private lateinit var imageService: ImageService
    @MockK private lateinit var convertImageToWebpUseCase: ConvertImageToWebpUseCase
    @InjectMockKs private lateinit var getAttachmentUseCase: GetAttachmentUseCaseImpl

    private val ownerId = Uuid.random()
    private val payload = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val sourceUrl = "https://example.test/thumbnail.png"
    private val image = ImageMockKBuilder {
        id = Uuid.random()
        createdAt = TimeUtils.now()
        updatedAt = TimeUtils.now()
        entityId = ownerId
        type = ImageType.THUMBNAIL
        url = sourceUrl
    }.build()

    @Nested
    @DisplayName("tests for the 'execute' method")
    inner class ExecuteTests {
        @Test
        @DisplayName("should return not found when the image is missing")
        fun `should return not found when the image is missing`() =
            runBlocking {
                // Given
                every {
                    imageService.findByEntityIdAndType(ownerId, ImageType.THUMBNAIL)
                } returns null

                // When
                val result = getAttachmentUseCase.execute(ownerId, ImageType.THUMBNAIL)

                // Then
                assertEquals(GetAttachmentResult.NotFound, result)
            }

        @Test
        @DisplayName("should return the encoded picture")
        fun `should return the encoded picture`() =
            runBlocking {
                // Given
                every {
                    imageService.findByEntityIdAndType(ownerId, ImageType.THUMBNAIL)
                } returns image
                coEvery {
                    convertImageToWebpUseCase.convert(sourceUrl, 80)
                } returns payload

                // When
                val result = getAttachmentUseCase.execute(ownerId, ImageType.THUMBNAIL)

                // Then
                val found = assertIs<GetAttachmentResult.Found>(result)
                assertContentEquals(payload, found.payload)
                assertTrue(found.etag.isNotBlank())
            }

        @Test
        @DisplayName("should skip the conversion when the validator matches")
        fun `should skip the conversion when the validator matches`() =
            runBlocking {
                // Given
                every {
                    imageService.findByEntityIdAndType(ownerId, ImageType.THUMBNAIL)
                } returns image
                coEvery {
                    convertImageToWebpUseCase.convert(sourceUrl, 80)
                } returns payload
                val first = assertIs<GetAttachmentResult.Found>(
                    getAttachmentUseCase.execute(ownerId, ImageType.THUMBNAIL)
                )

                // When
                val second = getAttachmentUseCase.execute(
                    ownerId,
                    ImageType.THUMBNAIL,
                    "\"${first.etag}\""
                )

                // Then
                assertEquals(GetAttachmentResult.NotModified(first.etag), second)
                coVerify(exactly = 1) { convertImageToWebpUseCase.convert(sourceUrl, 80) }
            }

        @Test
        @DisplayName("should return unavailable when the converter fails")
        fun `should return unavailable when the converter fails`() =
            runBlocking {
                // Given
                every {
                    imageService.findByEntityIdAndType(ownerId, ImageType.THUMBNAIL)
                } returns image
                coEvery {
                    convertImageToWebpUseCase.convert(sourceUrl, 80)
                } throws WebpConversionException("The picture could not be encoded")

                // When
                val result = getAttachmentUseCase.execute(ownerId, ImageType.THUMBNAIL)

                // Then
                assertEquals(
                    GetAttachmentResult.Unavailable("The picture could not be encoded"),
                    result
                )
            }
    }
}
