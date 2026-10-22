package fr.shikkanime.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("tests for the title normalization")
class NameNormalizerTest {

    @Nested
    @DisplayName("tests for the 'normalizeTitle' function")
    inner class NormalizeTitleTests {

        @Test
        fun `should keep the short name the historical rules produced`() {
            // Given: the titles harvested so far, with the short name `getShortName` gave on master
            val titlesToShortNames = historicalShortNames()

            titlesToShortNames.forEach { (fullTitle, expectedShortName) ->
                // When
                val shortName = normalizeTitle(fullTitle)

                // Then
                assertEquals(expectedShortName, shortName, "short name of '$fullTitle'")
            }
        }
    }

    /**
     * Reads the fixture of the harvested titles, one `full title` and `short name` pair per line,
     * separated by a tab.
     *
     * @return every harvested title with the short name the historical rules produced for it.
     * @see [normalizeTitle]
     */
    private fun historicalShortNames(): List<Pair<String, String>> =
        checkNotNull(NameNormalizerTest::class.java.getResource(HISTORICAL_SHORT_NAMES)) {
            "Missing test resource '$HISTORICAL_SHORT_NAMES'"
        }.readText()
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val (fullTitle, shortName) = line.split("\t", limit = 2)
                fullTitle to shortName
            }
            .toList()

    companion object {
        private const val HISTORICAL_SHORT_NAMES = "/name-normalizer/historical-short-names.tsv"
    }
}
