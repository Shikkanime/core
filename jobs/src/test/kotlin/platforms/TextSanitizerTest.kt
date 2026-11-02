package fr.shikkanime.jobs.platforms

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("tests for sanitizeText")
class TextSanitizerTest {

    @Test
    @DisplayName("should return null when the text is blank")
    fun `should return null when the text is blank`() {
        // Given
        val missing: String? = null
        val blank = "   "

        // Then
        assertNull(missing.sanitizeText())
        assertNull(blank.sanitizeText())
    }

    @Test
    @DisplayName("should drop the line breaks")
    fun `should drop the line breaks`() {
        // Given
        val text = "A baseball story\nwith two lines"

        // Then
        assertEquals("A baseball storywith two lines", text.sanitizeText())
    }

    @Test
    @DisplayName("should replace the non-breaking spaces with regular ones")
    fun `should replace the non-breaking spaces with regular ones`() {
        // Given
        val text = "Épisode\u00A05"

        // Then
        assertEquals("Épisode 5", text.sanitizeText())
    }

    @Test
    @DisplayName("should keep a clean text unchanged")
    fun `should keep a clean text unchanged`() {
        // Given
        val text = "One Piece"

        // Then
        assertEquals("One Piece", text.sanitizeText())
    }
}
