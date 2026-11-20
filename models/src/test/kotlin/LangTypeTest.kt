package fr.shikkanime.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Regression spec of the per-market audio locale rules ported from the legacy
 * `CountryCode`/`LocaleUtils`/`LangType` (branch `new-group-indexer`):
 * CJK locales are the subtitles, the country's own locale is the voice, and
 * English is only a fallback when no CJK locale is available.
 */
@DisplayName("tests for the market language types")
class LangTypeTest {

    @Nested
    @DisplayName("fromAudioLocale")
    inner class FromAudioLocaleTests {

        @Test
        fun `should map the country voice locale to VOICE`() {
            assertEquals(LangType.VOICE, LangType.fromAudioLocale(Country.FR, Locale.FR_FR))
            assertEquals(LangType.VOICE, LangType.fromAudioLocale(Country.DE, Locale.DE_DE))
            assertEquals(LangType.VOICE, LangType.fromAudioLocale(Country.BE, Locale.FR_BE))
        }

        @Test
        fun `should map any other visible locale to SUBTITLES`() {
            assertEquals(LangType.SUBTITLES, LangType.fromAudioLocale(Country.FR, Locale.JA_JP))
            assertEquals(LangType.SUBTITLES, LangType.fromAudioLocale(Country.FR, Locale.EN_US))
            assertEquals(LangType.SUBTITLES, LangType.fromAudioLocale(Country.FR, Locale.ZH_CN))
        }

        @Test
        fun `should map a foreign voice locale to SUBTITLES`() {
            // fr-BE is the voice of BE, not of FR: for FR it is just another
            // (invisible) locale — filtering happens upstream, the mapping
            // itself stays mechanical like the legacy `fromAudioLocale`
            assertEquals(LangType.SUBTITLES, LangType.fromAudioLocale(Country.FR, Locale.FR_BE))
            assertEquals(LangType.SUBTITLES, LangType.fromAudioLocale(Country.FR, Locale.DE_DE))
            assertEquals(LangType.SUBTITLES, LangType.fromAudioLocale(Country.DE, Locale.FR_FR))
        }
    }

    @Nested
    @DisplayName("visible audio locales per country")
    inner class VisibleLocalesTests {

        @Test
        fun `should keep CJK subtitles and the voice locale`() {
            // Given: every locale available on an episode
            val available = Locale.entries.toSet()

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then: CJK + voice, no EN (CJK wins the subtitle slot)
            assertEquals(
                setOf(Locale.JA_JP, Locale.ZH_CN, Locale.KO_KR, Locale.FR_FR),
                visible
            )
        }

        @Test
        fun `should fall back to english when no CJK locale is available`() {
            // Given: only en-US and fr-FR on the episode
            val available = setOf(Locale.EN_US, Locale.FR_FR)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then
            assertEquals(setOf(Locale.EN_US, Locale.FR_FR), visible)
        }

        @Test
        fun `should drop english when a CJK locale is available`() {
            // Given: en-US + ja-JP + fr-FR
            val available = setOf(Locale.EN_US, Locale.JA_JP, Locale.FR_FR)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then: en-US excluded, ja-JP kept as subtitle
            assertEquals(setOf(Locale.JA_JP, Locale.FR_FR), visible)
        }

        @Test
        fun `should keep only the CJK locales that are available`() {
            // Given: ja-JP + ko-KR + fr-FR (no zh-CN)
            val available = setOf(Locale.JA_JP, Locale.KO_KR, Locale.FR_FR)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then
            assertEquals(setOf(Locale.JA_JP, Locale.KO_KR, Locale.FR_FR), visible)
        }

        @Test
        fun `should exclude locales of other markets`() {
            // Given: de-DE + pl-PL + fr-FR — for FR neither de nor pl is visible
            val available = setOf(Locale.DE_DE, Locale.PL_PL, Locale.FR_FR, Locale.EN_US)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then: only en (fallback, no CJK) + fr voice
            assertEquals(setOf(Locale.EN_US, Locale.FR_FR), visible)
        }

        @Test
        fun `should use the country own locale as voice for every market`() {
            Country.entries.forEach { country ->
                val available = setOf(country.defaultLocale, Locale.JA_JP)

                val visible = country.visibleAudioLocales(available)

                assertTrue(country.defaultLocale in visible, "${country.code} voice missing")
                assertTrue(Locale.JA_JP in visible, "${country.code} subtitle missing")
                assertFalse(Locale.EN_US in visible)
            }
        }

        @Test
        fun `should return an empty set when nothing is available`() {
            assertTrue(Country.FR.visibleAudioLocales(emptySet()).isEmpty())
        }

        @Test
        fun `should keep english alone as the subtitle fallback`() {
            // Given: only en-US on the episode
            val available = setOf(Locale.EN_US)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then
            assertEquals(setOf(Locale.EN_US), visible)
        }

        @Test
        fun `should keep a CJK locale without any voice`() {
            // Given: only ja-JP on the episode
            val available = setOf(Locale.JA_JP)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then: CJK wins, no EN fallback needed
            assertEquals(setOf(Locale.JA_JP), visible)
        }

        @Test
        fun `should drop english when CJK is available even without voice`() {
            // Given: en-US + ja-JP, no fr-FR voice on the episode
            val available = setOf(Locale.EN_US, Locale.JA_JP)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then
            assertEquals(setOf(Locale.JA_JP), visible)
        }

        @Test
        fun `should reject a sibling market locale`() {
            // Given: only fr-BE — the voice of BE, meaningless for FR
            val available = setOf(Locale.FR_BE)

            // When
            val visible = Country.FR.visibleAudioLocales(available)

            // Then: nothing is visible for FR
            assertTrue(visible.isEmpty())
        }

        @Test
        fun `should not expose another market voice locale`() {
            // Given: fr-FR + en-US viewed from Germany — fr-FR is not the DE
            // voice and not a subtitle locale, only the EN fallback survives
            val visible = Country.DE.visibleAudioLocales(setOf(Locale.FR_FR, Locale.EN_US))

            // Then
            assertEquals(setOf(Locale.EN_US), visible)
        }

        @Test
        fun `should require the exact voice locale of each market`() {
            // Given: only fr-FR — the voice of FR but not of BE (fr-BE)
            val visible = Country.BE.visibleAudioLocales(setOf(Locale.FR_FR))

            // Then: nothing is visible for BE
            assertTrue(visible.isEmpty())
        }
    }
}
