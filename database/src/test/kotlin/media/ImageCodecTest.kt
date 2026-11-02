package fr.shikkanime.database.media

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

@DisplayName("tests for the WebP codec")
class ImageCodecTest {

    @Nested
    @DisplayName("tests for the 'encodeLossy' function")
    inner class EncodeLossyTests {

        @Test
        fun `should keep the channel order of a plain photograph`() {
            // Given
            val source = solid(64, 96, red = 220, green = 30, blue = 40)

            // When
            val decoded = ImageCodec.decode(ImageCodec.encodeLossy(source, QUALITY))

            // Then
            assertEquals(64, decoded.width)
            assertEquals(96, decoded.height)
            val pixel = decoded.getRGB(32, 48)
            assertTrue(red(pixel) > 200 && green(pixel) < 60 && blue(pixel) < 70) { "channels are inverted: $pixel" }
        }

        @Test
        fun `should keep every alpha value of a transparent picture`() {
            // Given
            val source = alphaGradient(128, 8)

            // When
            val decoded = ImageCodec.decode(ImageCodec.encodeLossy(source, ALPHA_QUALITY))

            // Then
            for (x in 0 until source.width)
                assertEquals(source.getRGB(x, 4) ushr 24 and 0xFF, decoded.getRGB(x, 4) ushr 24 and 0xFF) {
                    "alpha of column $x is not exact"
                }
        }

        @Test
        fun `should keep a transparent background fully transparent`() {
            // Given
            val source = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)

            // When
            val decoded = ImageCodec.decode(ImageCodec.encodeLossy(source, ALPHA_QUALITY))

            // Then
            assertEquals(0, decoded.getRGB(0, 0) ushr 24 and 0xFF)
            assertEquals(0, decoded.getRGB(31, 31) ushr 24 and 0xFF)
        }
    }

    @Nested
    @DisplayName("tests for the 'encodeLossless' function")
    inner class EncodeLosslessTests {

        @Test
        fun `should keep a logo pixel-perfect`() {
            // Given
            val source = checkerboard(64, 64)

            // When
            val decoded = ImageCodec.decode(ImageCodec.encodeLossless(source))

            // Then
            for (y in 0 until source.height step 7)
                for (x in 0 until source.width step 7)
                    assertEquals(source.getRGB(x, y), decoded.getRGB(x, y)) { "pixel ($x, $y) changed" }
        }

        @Test
        fun `should keep the transparency of a logo`() {
            // Given
            val source = alphaGradient(64, 64)

            // When
            val decoded = ImageCodec.decode(ImageCodec.encodeLossless(source))

            // Then
            for (y in 0 until source.height step 9)
                for (x in 0 until source.width step 9)
                    assertEquals(source.getRGB(x, y) ushr 24 and 0xFF, decoded.getRGB(x, y) ushr 24 and 0xFF)
        }
    }

    @Nested
    @DisplayName("tests for the 'resize' function")
    inner class ResizeTests {

        @Test
        fun `should resize to the requested dimensions`() {
            // Given
            val source = checkerboard(400, 600)

            // When
            val resized = ImageCodec.resize(source, 100, 150)

            // Then
            assertEquals(100, resized.width)
            assertEquals(150, resized.height)
        }

        @Test
        fun `should keep the ratio when the target height is unknown`() {
            // Given
            val source = checkerboard(400, 600)

            // When
            val resized = ImageCodec.resize(source, 200, -1)

            // Then
            assertEquals(200, resized.width)
            assertEquals(300, resized.height)
        }

        @Test
        fun `should not darken the edges of a transparent picture`() {
            // Given
            val source = BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB)
            val graphics = source.createGraphics()
            graphics.color = java.awt.Color.RED
            graphics.fillRect(150, 150, 100, 100)
            graphics.dispose()

            // When
            val resized = ImageCodec.resize(source, 100, 100)

            // Then
            val corner = resized.getRGB(0, 0)
            assertEquals(0, corner ushr 24 and 0xFF) { "a transparent corner became visible: $corner" }
        }
    }

    private fun solid(width: Int, height: Int, red: Int, green: Int, blue: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color(red, green, blue)
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        return image
    }

    private fun alphaGradient(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        for (y in 0 until height)
            for (x in 0 until width)
                image.setRGB(x, y, ((x * 255 / (width - 1)) shl 24) or 0x3366CC)

        return image
    }

    private fun checkerboard(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        for (y in 0 until height)
            for (x in 0 until width)
                image.setRGB(
                    x,
                    y,
                    if ((x / 8 + y / 8) % 2 == 0) 0xFF1020E0.toInt() else 0x8030C040.toInt()
                )

        return image
    }

    private fun red(pixel: Int) = pixel shr 16 and 0xFF
    private fun green(pixel: Int) = pixel shr 8 and 0xFF
    private fun blue(pixel: Int) = pixel and 0xFF

    companion object {
        private const val QUALITY = 80
        private const val ALPHA_QUALITY = 85
    }
}
