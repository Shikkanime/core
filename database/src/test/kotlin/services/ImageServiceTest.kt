package fr.shikkanime.database.services

import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.ImageMaterialization
import fr.shikkanime.database.repositories.ImagePayloadRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.impl.ImageServiceImpl
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.uuid.Uuid

private const val ARTWORK_URL = "https://cdn.example/artwork.png"

@DisplayName("tests for the image service")
@ExtendWith(MockKExtension::class)
class ImageServiceTest {
    @MockK private lateinit var imageRepository: ImageRepository
    @MockK private lateinit var imagePayloadRepository: ImagePayloadRepository
    @InjectMockKs private lateinit var imageService: ImageServiceImpl

    private val owner: Uuid = Uuid.random()
    private val imageId: Uuid = Uuid.random()

    @Nested
    @DisplayName("tests for the 'store' method")
    inner class StoreTests {

        @Test
        fun `should encode the artwork at the canonical dimensions of its type`() {
            // Given
            val image = givenImage()
            val materialization = slot<ImageMaterialization>()

            every { imageRepository.markMaterialized(imageId, ARTWORK_URL, capture(materialization)) } returns true
            every { imagePayloadRepository.save(imageId, any()) } just Runs

            // When
            val stored = imageService.store(image, ImageSource(png(), null, null))

            // Then
            assertEquals(ImageType.THUMBNAIL.width, materialization.captured.width)
            assertEquals(ImageType.THUMBNAIL.height, materialization.captured.height)
            assertTrue(materialization.captured.version.isNotBlank())
            assertEquals("RIFF", String(stored.bytes, 0, 4, Charsets.US_ASCII))
            assertEquals(materialization.captured.version, stored.image.version)
        }

        @Test
        fun `should publish the same version for the same artwork`() {
            // Given
            val image = givenImage()
            val versions = mutableListOf<String>()
            val bytes = png()

            every { imageRepository.markMaterialized(imageId, ARTWORK_URL, any()) } answers {
                versions += arg<ImageMaterialization>(2).version
                true
            }
            every { imagePayloadRepository.save(imageId, any()) } just Runs

            // When
            imageService.store(image, ImageSource(bytes, null, null))
            imageService.store(image, ImageSource(bytes, null, null))

            // Then
            assertEquals(2, versions.size)
            assertEquals(versions[0], versions[1])
        }

        @Test
        fun `should refuse an artwork whose source moved while it was fetched`() {
            // Given
            val image = givenImage()
            every { imageRepository.markMaterialized(imageId, ARTWORK_URL, any()) } returns false

            // When / Then
            assertThrows<IllegalArgumentException> {
                imageService.store(image, ImageSource(png(), null, null))
            }

            verify(exactly = 0) { imagePayloadRepository.save(any(), any()) }
        }
    }

    @Nested
    @DisplayName("tests for the 'loadImage' method")
    inner class LoadImageTests {

        @Test
        fun `should decode the stored artwork without asking the platform`() {
            // Given
            val image = givenImage()

            every { imageRepository.findByEntityIdAndType(owner, ImageType.THUMBNAIL) } returns image
            every { imagePayloadRepository.findByImageId(imageId) } returns png(transparent = true)

            // When
            val loaded = imageService.loadImage(owner, ImageType.THUMBNAIL)

            // Then
            assertNotNull(loaded)
            assertTrue(loaded!!.colorModel.hasAlpha())
        }

        @Test
        fun `should return null when the artwork was never materialized`() {
            // Given
            val image = givenImage()

            every { imageRepository.findByEntityIdAndType(owner, ImageType.THUMBNAIL) } returns image
            every { imagePayloadRepository.findByImageId(imageId) } returns null

            // When
            val loaded = imageService.loadImage(owner, ImageType.THUMBNAIL)

            // Then
            assertNull(loaded)
        }
    }

    @Nested
    @DisplayName("tests for the 'findWithPayload' method")
    inner class FindWithPayloadTests {

        @Test
        fun `should read the payload when the requested version matches`() {
            // Given
            val image = givenImage(version = PUBLISHED_VERSION)
            val payload = byteArrayOf(1, 2, 3)

            every { imageRepository.findByEntityIdAndType(owner, ImageType.THUMBNAIL) } returns image
            every { imagePayloadRepository.findByImageId(imageId) } returns payload

            // When
            val found = imageService.findWithPayload(owner, ImageType.THUMBNAIL, PUBLISHED_VERSION)

            // Then
            assertEquals(payload, found?.payload)
        }

        @Test
        fun `should not read the payload when the version is superseded`() {
            // Given
            val image = givenImage(version = PUBLISHED_VERSION)

            every { imageRepository.findByEntityIdAndType(owner, ImageType.THUMBNAIL) } returns image

            // When
            val found = imageService.findWithPayload(owner, ImageType.THUMBNAIL, "another-version")

            // Then
            assertNull(found?.payload)
            verify(exactly = 0) { imagePayloadRepository.findByImageId(any()) }
        }
    }

    private fun givenImage(version: String? = null): Image =
        ImageMockKBuilder {
            id = imageId
            entityId = owner
            type = ImageType.THUMBNAIL
            url = ARTWORK_URL
            this.version = version
        }.build()

    private fun png(transparent: Boolean = false): ByteArray {
        val type = if (transparent) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val buffered = BufferedImage(120, 180, type)
        val graphics = buffered.createGraphics()
        graphics.color = Color(200, 40, 40, if (transparent) 128 else 255)
        graphics.fillRect(0, 0, buffered.width, buffered.height)
        graphics.dispose()

        val output = ByteArrayOutputStream()
        ImageIO.write(buffered, "png", output)
        return output.toByteArray()
    }

    private companion object {
        const val PUBLISHED_VERSION = "d949711bb189064f"
    }
}
