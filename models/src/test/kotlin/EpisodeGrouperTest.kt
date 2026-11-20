package fr.shikkanime.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Regression spec of the episode grouping semantics ported from the legacy
 * `GroupedIndexer`/`NewGroupIndexer` (branch `new-group-indexer`).
 *
 * The tests describe the expected behaviour of [EpisodeGrouper.group] on the
 * domain observations below; they compile and pass once the grouper is
 * implemented in `:models`.
 */
@DisplayName("tests for the episode grouper")
class EpisodeGrouperTest {

    private val animeId = Uuid.random()
    private val animeSlug = "test-anime"
    private val base = LocalDateTime.of(2026, 1, 10, 20, 0)

    private fun obs(
        episodeId: Uuid = Uuid.random(),
        season: Int = 1,
        episodeType: EpisodeType = EpisodeType.EPISODE,
        number: Int,
        audioLocale: Locale = Locale.FR_FR,
        releaseDateTime: LocalDateTime = base,
        releaseId: Uuid = Uuid.random(),
        slug: String = animeSlug,
        anime: Uuid = animeId
    ) = EpisodeObservation(
        animeId = anime,
        animeSlug = slug,
        episodeId = episodeId,
        season = season,
        episodeType = episodeType,
        number = number,
        releaseId = releaseId,
        audioLocale = audioLocale,
        releaseDateTime = releaseDateTime
    )

    private fun group(
        vararg observations: EpisodeObservation,
        country: Country = Country.FR
    ): List<EpisodeGroup> = EpisodeGrouper.group(observations.toList(), country)

    @Nested
    @DisplayName("bucket window of plus or minus two hours")
    inner class WindowTests {

        @Test
        fun `should merge releases of the same anime and type within two hours`() {
            // Given: a subtitle release and a voice release one hour apart
            val episode = Uuid.random()
            val sub = obs(episodeId = episode, number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base)
            val voice = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base.plusHours(1))

            // When
            val groups = group(sub, voice)

            // Then: one single group holds both releases
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should split releases beyond the two hour window`() {
            // Given: two releases three hours apart
            val first = obs(number = 1, releaseDateTime = base)
            val second = obs(number = 2, releaseDateTime = base.plusHours(3))

            // When
            val groups = group(first, second)

            // Then
            assertEquals(2, groups.size)
        }

        @Test
        fun `should anchor the window on the first observation, not transitively`() {
            // Given: A@0h, B@+1h50 (joins A's bucket), C@+3h50
            // C is within 2h of B but 3h50 of the anchor A: legacy semantics
            // keep it out of A's bucket.
            val a = obs(number = 1, releaseDateTime = base)
            val b = obs(number = 2, releaseDateTime = base.plusHours(1).plusMinutes(50))
            val c = obs(number = 3, releaseDateTime = base.plusHours(3).plusMinutes(50))

            // When
            val groups = group(a, b, c)

            // Then: two groups — {A,B} anchored at A and {C}
            assertEquals(2, groups.size)
            assertEquals(setOf(1, 2), groups.first { it.releases.size == 2 }.episodes.map { it.number }.toSet())
            assertEquals(listOf(3), groups.first { it.releases.size == 1 }.episodes.map { it.number })
        }

        @Test
        fun `should produce the same groups whatever the input order`() {
            // Given: the same observations shuffled
            val observations = listOf(
                obs(number = 1, releaseDateTime = base),
                obs(number = 2, releaseDateTime = base.plusHours(1)),
                obs(number = 3, releaseDateTime = base.plusHours(5))
            )

            // When
            val forward = EpisodeGrouper.group(observations, Country.FR)
            val backward = EpisodeGrouper.group(observations.reversed(), Country.FR)

            // Then: deterministic output independent of insertion order
            assertEquals(forward, backward)
        }

        @Test
        fun `should not merge releases of different animes or episode types`() {
            // Given: same slot, different anime / different episode type
            val episode = obs(number = 1, releaseDateTime = base)
            val otherAnime = obs(number = 1, releaseDateTime = base, anime = Uuid.random(), slug = "other")
            val film = obs(number = 1, releaseDateTime = base, episodeType = EpisodeType.FILM)

            // When
            val groups = group(episode, otherAnime, film)

            // Then
            assertEquals(3, groups.size)
        }
    }

    @Nested
    @DisplayName("coverage signature split (LangType)")
    inner class CoverageTests {

        @Test
        fun `should split a bucket when language coverages differ`() {
            // Given: episode 1 has SUB+VOICE, episode 2 has SUB only —
            // same anime, same type, same 2h window
            val ep1Sub = obs(episodeId = Uuid.random(), number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base)
            val ep1Voice = obs(episodeId = ep1Sub.episodeId, number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base)
            val ep2Sub = obs(episodeId = Uuid.random(), number = 2, audioLocale = Locale.JA_JP, releaseDateTime = base)

            // When
            val groups = group(ep1Sub, ep1Voice, ep2Sub)

            // Then: two groups — {ep1 SUB+VOICE} and {ep2 SUB}
            assertEquals(2, groups.size)
        }

        @Test
        fun `should keep episodes with identical coverage in one group`() {
            // Given: two episodes both covered SUB+VOICE
            val ep1 = Uuid.random()
            val ep2 = Uuid.random()
            val observations = listOf(
                obs(episodeId = ep1, number = 1, audioLocale = Locale.JA_JP),
                obs(episodeId = ep1, number = 1, audioLocale = Locale.FR_FR),
                obs(episodeId = ep2, number = 2, audioLocale = Locale.JA_JP),
                obs(episodeId = ep2, number = 2, audioLocale = Locale.FR_FR)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then: one group with both episodes
            assertEquals(1, groups.size)
            assertEquals(setOf(1, 2), groups.single().episodes.map { it.number }.toSet())
        }

        @Test
        fun `should exclude releases whose locale is not visible for the country`() {
            // Given: for Country.FR the visible locales are CJK subtitles (or EN
            // fallback) plus fr-FR voice; a de-DE release must be excluded
            val fr = obs(number = 1, audioLocale = Locale.FR_FR)
            val sub = obs(number = 1, audioLocale = Locale.JA_JP)
            val german = obs(number = 1, audioLocale = Locale.DE_DE)

            // When
            val groups = group(fr, sub, german, country = Country.FR)

            // Then: the group only carries the fr-FR and ja-JP releases
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should fall back to english subtitles when no CJK locale is available`() {
            // Given: an episode with en-US and fr-FR releases only
            val en = obs(number = 1, audioLocale = Locale.EN_US)
            val fr = obs(number = 1, audioLocale = Locale.FR_FR)

            // When
            val groups = group(en, fr)

            // Then: en-US counts as SUBTITLES, the group exists
            assertEquals(1, groups.size)
            assertTrue(groups.single().langTypes.containsKey(LangType.SUBTITLES))
        }

        @Test
        fun `should not use english subtitles when a CJK locale is available`() {
            // Given: en-US + ja-JP + fr-FR on the same episode —
            // ja-JP wins the subtitle slot, en-US is excluded for FR
            val en = obs(number = 1, audioLocale = Locale.EN_US)
            val ja = obs(number = 1, audioLocale = Locale.JA_JP)
            val fr = obs(number = 1, audioLocale = Locale.FR_FR)

            // When
            val groups = group(en, ja, fr)

            // Then: only ja-JP (SUB) + fr-FR (VOICE) remain
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }
    }

    @Nested
    @DisplayName("group content")
    inner class ContentTests {

        @Test
        fun `should expose the anchor release date of the group`() {
            // Given: releases at +0h and +1h
            val first = obs(number = 1, releaseDateTime = base)
            val second = obs(number = 1, releaseDateTime = base.plusHours(1))

            // When
            val group = group(first, second).single()

            // Then: the group date is the anchor (earliest in window order)
            assertEquals(base, group.releaseDateTime)
        }

        @Test
        fun `should deduplicate releases seen twice`() {
            // Given: the exact same release submitted twice (idempotent re-save)
            val release = obs(number = 1)

            // When
            val groups = group(release, release)

            // Then: a single release inside the group, no duplicate
            assertEquals(1, groups.size)
            assertEquals(1, groups.single().releases.size)
        }

        @Test
        fun `should order groups by release date, slug then episode type`() {
            // Given: groups on different dates and animes
            val later = obs(number = 1, releaseDateTime = base.plusDays(1), slug = "b-anime", anime = Uuid.random())
            val earlier = obs(number = 1, releaseDateTime = base, slug = "a-anime", anime = Uuid.random())
            val sameDate = obs(number = 1, releaseDateTime = base, slug = "z-anime", anime = Uuid.random())

            // When
            val groups = group(later, earlier, sameDate)

            // Then
            assertEquals(listOf("a-anime", "z-anime", "b-anime"), groups.map { it.animeSlug })
        }
    }
}
