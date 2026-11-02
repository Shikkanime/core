package fr.shikkanime.media.webp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

private const val QUALITY = 0.8f
private const val LOW_QUALITY = 0.1f
private const val HIGH_QUALITY = 0.95f

@DisplayName("tests for the WebP ImageIO plugin")
class WebpImageIoTest {

    @Nested
    @DisplayName("tests for writing and reading through ImageIO")
    inner class RoundTripTests {

        @Test
        fun `should write and read a picture through ImageIO`() {
            // Given
            val source = solid(120, 180, Color(220, 30, 40))

            // When
            val decoded = ImageIO.read(ByteArrayInputStream(webp(source)))

            // Then
            assertNotNull(decoded)
            assertEquals(120, decoded!!.width)
            assertEquals(180, decoded.height)
            assertTrue(decoded.getRGB(60, 90) shr 16 and 0xFF > 200) { "The channels are inverted" }
        }

        @Test
        fun `should keep every alpha value of a transparent picture`() {
            // Given
            val source = alphaGradient(128, 8)

            // When
            val decoded = ImageIO.read(ByteArrayInputStream(webp(source)))

            // Then
            for (x in 0 until source.width)
                assertEquals(source.getRGB(x, 4) ushr 24 and 0xFF, decoded.getRGB(x, 4) ushr 24 and 0xFF) {
                    "The alpha of column $x is not exact"
                }
        }

        @Test
        fun `should keep a picture pixel-perfect when writing lossless`() {
            // Given
            val source = checkerboard(64, 64)

            // When
            val decoded = ImageIO.read(ByteArrayInputStream(webp(source, lossless = true)))

            // Then
            for (y in 0 until source.height step 7)
                for (x in 0 until source.width step 7)
                    assertEquals(source.getRGB(x, y), decoded.getRGB(x, y)) { "The pixel ($x, $y) changed" }
        }
    }

    @Nested
    @DisplayName("tests for the reader")
    inner class ReaderTests {

        @Test
        fun `should answer the size without decoding`() {
            // Given
            val payload = webp(solid(300, 200, Color.BLUE))

            // When
            ImageIO.createImageInputStream(ByteArrayInputStream(payload)).use { stream ->
                val reader = ImageIO.getImageReaders(stream).next()
                reader.input = stream

                // Then
                assertEquals(300, reader.getWidth(0))
                assertEquals(200, reader.getHeight(0))
                reader.dispose()
            }
        }

        @Test
        fun `should announce RGB for an opaque picture`() {
            // Given
            val payload = webp(solid(40, 40, Color.GREEN))

            // When
            ImageIO.createImageInputStream(ByteArrayInputStream(payload)).use { stream ->
                val reader = ImageIO.getImageReaders(stream).next()
                reader.input = stream

                // Then
                assertEquals(
                    BufferedImage.TYPE_INT_RGB,
                    reader.getImageTypes(0).next().bufferedImageType
                )
                reader.dispose()
            }
        }

        @Test
        fun `should announce ARGB for a transparent picture`() {
            // Given
            val payload = webp(alphaGradient(40, 40))

            // When
            ImageIO.createImageInputStream(ByteArrayInputStream(payload)).use { stream ->
                val reader = ImageIO.getImageReaders(stream).next()
                reader.input = stream

                // Then
                assertEquals(
                    BufferedImage.TYPE_INT_ARGB,
                    reader.getImageTypes(0).next().bufferedImageType
                )
                reader.dispose()
            }
        }
    }

    @Nested
    @DisplayName("tests for the writer")
    inner class WriterTests {

        @Test
        fun `should spend fewer bytes at a lower quality`() {
            // Given
            val source = noise(200, 200)

            // When
            val low = webp(source, quality = LOW_QUALITY).size
            val high = webp(source, quality = HIGH_QUALITY).size

            // Then
            assertTrue(low < high) { "The low quality payload ($low) is not smaller than $high" }
        }

        @Test
        fun `should not store an alpha plane for an opaque picture`() {
            // Given
            val payload = webp(solid(40, 40, Color.WHITE))

            // Then
            assertFalse(LibWebp.readInfo(payload).hasAlpha)
        }

        @Test
        fun `should store an alpha plane for a transparent picture`() {
            // Given
            val payload = webp(alphaGradient(40, 40))

            // Then
            assertTrue(LibWebp.readInfo(payload).hasAlpha)
        }
    }

    @Nested
    @DisplayName("tests for the codec availability")
    inner class AvailabilityTests {

        @Test
        fun `should report libwebp as loaded when the codec works`() {
            // Then
            assertTrue(LibWebp.isAvailable())
        }
    }

    /**
     * Encodes a picture through the writer ImageIO registered.
     *
     * @param image picture to encode.
     * @param quality compression quality, from 0 to 1.
     * @param lossless whether the payload must be encoded without loss.
     * @return the encoded payload.
     * @see [LibWebp]
     */
    private fun webp(
        image: BufferedImage,
        quality: Float = QUALITY,
        lossless: Boolean = false
    ): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("webp").next()
        val param = writer.defaultWriteParam as WebpWriteParam

        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
        param.compressionType = if (lossless) WebpWriteParam.LOSSLESS else WebpWriteParam.LOSSY
        param.compressionQuality = quality

        val output = ByteArrayOutputStream()

        ImageIO.createImageOutputStream(output).use { stream ->
            writer.output = stream
            writer.write(null, IIOImage(image, null, null), param)
        }

        writer.dispose()
        return output.toByteArray()
    }

    /**
     * Builds a plain picture filled with one colour.
     *
     * @param width width of the picture.
     * @param height height of the picture.
     * @param color colour filling the picture.
     * @return the filled picture.
     * @see [LibWebp]
     */
    private fun solid(width: Int, height: Int, color: Color): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        return image
    }

    /**
     * Builds a picture whose alpha grows from transparent to opaque.
     *
     * @param width width of the picture.
     * @param height height of the picture.
     * @return the gradient picture.
     * @see [LibWebp]
     */
    private fun alphaGradient(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        for (y in 0 until height)
            for (x in 0 until width)
                image.setRGB(x, y, ((x * 255 / (width - 1)) shl 24) or 0x3366CC)

        return image
    }

    /**
     * Builds a transparent picture with a two-tone checkerboard.
     *
     * @param width width of the picture.
     * @param height height of the picture.
     * @return the checkerboard picture.
     * @see [LibWebp]
     */
    private fun checkerboard(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        for (y in 0 until height)
            for (x in 0 until width)
                image.setRGB(x, y, if ((x / 8 + y / 8) % 2 == 0) 0xFF1020E0.toInt() else 0x8030C040.toInt())

        return image
    }

    /**
     * Builds a picture of random pixels, the worst case for a lossy encoder.
     *
     * @param width width of the picture.
     * @param height height of the picture.
     * @return the noisy picture.
     * @see [LibWebp]
     */
    private fun noise(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val random = Random(42)

        for (y in 0 until height)
            for (x in 0 until width)
                image.setRGB(x, y, random.nextInt(0xFFFFFF))

        return image
    }
}
