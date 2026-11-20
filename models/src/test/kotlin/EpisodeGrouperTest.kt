package fr.shikkanime.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

/** Test helper: LocalDateTime carries no arithmetic — roundtrip through UTC Instant. */
private operator fun LocalDateTime.plus(duration: Duration): LocalDateTime =
    with(TimeZone.UTC) { toInstant().plus(duration).toLocalDateTime() }

/**
 * Regression spec of the episode grouping semantics ported from the legacy
 * `GroupedIndexer`/`NewGroupIndexer` (branch `new-group-indexer`).
 *
 * Spec of [EpisodeGrouper.group] :
 * - input: flat [EpisodeObservation] rows (one per episode release), all
 *   evaluated under the single [Country] argument — the caller guarantees the
 *   rows are relevant for that market. Multi-country callers group per
 *   country and merge the results themselves;
 * - releases whose locale is not visible for the country are excluded
 *   (`Country.visibleAudioLocales`, resolved per episode on all its
 *   releases): they cannot create a group, serve as an anchor, nor feed the
 *   coverage signature;
 * - `searchTypes`, when non-empty, keeps only releases whose [LangType] is
 *   listed and drops the groups left empty. Filtering before the split is
 *   equivalent to filtering the result: every LangType owns its signature,
 *   so removing whole lang types never changes the remaining signatures;
 * - an episode is identified by its coordinates `(season, episodeType,
 *   number)` — `episodeId` is carried for reference only;
 * - observations are bucketed by `(animeId, episodeType)` when their
 *   `releaseDateTime` falls inside the ±2h window anchored on the bucket's
 *   first observation (inclusive bounds, deterministic: input order does not
 *   matter);
 * - inside a bucket, observations are split by coverage signature: the key of
 *   an observation is the set of episode coordinates covered by its
 *   [LangType] across the whole bucket — the `NewGroupIndexer` semantics;
 * - a group's `releaseDateTime` is the earliest release date of its own
 *   members (not necessarily the bucket anchor);
 * - a group's `animeSlug` is the lexicographically smallest member slug;
 * - group members are canonically ordered: releases by `(releaseDateTime,
 *   releaseId)`, episodes by `(season, episodeType, number)`;
 * - releases are deduplicated by `releaseId`: the observation with the
 *   earliest `releaseDateTime` wins (a re-observed release keeps its first
 *   sighting — the caller is expected to upsert, not re-insert);
 * - output: groups sorted ascending by `(releaseDateTime, animeSlug,
 *   episodeType, minSeason, minNumber)`, then by the group's LangType set
 *   compared by enum ordinal, then by the smallest member release id — a
 *   total deterministic order. The service reverses or re-orders for DESC
 *   requests and re-applies the legacy default sort
 *   `(releaseDateTime, animeName, minSeason, episodeType, minNumber)` for
 *   parity with the old endpoint.
 *
 * Out of scope: the weekly view rules (`AnimeService.getWeeklyAnimes`), the
 * v1 `GroupedIndexer.group()` consumers (media-image endpoint, social
 * notifications — single-market grouping only here), and pagination, which
 * live in the service/API layers.
 */
@DisplayName("tests for the episode grouper")
class EpisodeGrouperTest {

    private val animeId = Uuid.random()
    private val animeSlug = "test-anime"
    private val base = LocalDateTime(2026, 1, 10, 20, 0)

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
    ): EpisodeObservation = EpisodeObservation(
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
        country: Country = Country.FR,
        searchTypes: Set<LangType>? = null
    ): List<EpisodeGroup> = EpisodeGrouper.group(observations.toList(), country, searchTypes)

    @Nested
    @DisplayName("empty inputs")
    inner class EmptyTests {

        @Test
        fun `should return no group for an empty input`() {
            // When
            val groups = group()

            // Then
            assertTrue(groups.isEmpty())
        }

        @Test
        fun `should return no group when every release is filtered out`() {
            // Given: only invisible locales for FR
            val observations = listOf(
                obs(number = 1, audioLocale = Locale.DE_DE),
                obs(number = 2, audioLocale = Locale.PL_PL)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then
            assertTrue(groups.isEmpty())
        }

        @Test
        fun `should return no group when searchTypes matches nothing`() {
            // Given: an episode carrying only subtitle releases
            val episode = Uuid.random()
            val observations = listOf(
                obs(episodeId = episode, number = 1, audioLocale = Locale.JA_JP),
                obs(episodeId = episode, number = 1, audioLocale = Locale.ZH_CN)
            )

            // When: only VOICE releases are requested
            val groups = group(*observations.toTypedArray(), searchTypes = setOf(LangType.VOICE))

            // Then
            assertTrue(groups.isEmpty())
        }
    }

    @Nested
    @DisplayName("bucket window of plus or minus two hours")
    inner class WindowTests {

        @Test
        fun `should merge releases of the same anime and type within two hours`() {
            // Given: a subtitle release and a voice release one hour apart
            val sub = obs(number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base)
            val voice = obs(number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base + 1.hours)

            // When
            val groups = group(sub, voice)

            // Then: one single group holds both releases
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should merge a release exactly two hours after the anchor`() {
            // Given: the window bounds are inclusive on both sides
            val first = obs(number = 1, releaseDateTime = base)
            val edge = obs(number = 1, releaseDateTime = base + 2.hours)

            // When
            val groups = group(first, edge)

            // Then
            assertEquals(1, groups.size)
        }

        @Test
        fun `should split a release one second beyond two hours`() {
            // Given
            val first = obs(number = 1, releaseDateTime = base)
            val beyond = obs(number = 2, releaseDateTime = base + 2.hours + 1.seconds)

            // When
            val groups = group(first, beyond)

            // Then
            assertEquals(2, groups.size)
        }

        @Test
        fun `should split releases beyond the two hour window`() {
            // Given: two releases three hours apart
            val first = obs(number = 1, releaseDateTime = base)
            val second = obs(number = 2, releaseDateTime = base + 3.hours)

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
            val b = obs(number = 2, releaseDateTime = base + 1.hours + 50.minutes)
            val c = obs(number = 3, releaseDateTime = base + 3.hours + 50.minutes)

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
                obs(number = 2, releaseDateTime = base + 1.hours),
                obs(number = 3, releaseDateTime = base + 5.hours)
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

        @Test
        fun `should merge observations sharing the anime id whatever the slug says`() {
            // Given: same animeId but two different slugs — the slug is not
            // part of the grouping key (legacy deviation, locked here); the
            // group exposes the smallest slug deterministically
            val first = obs(number = 1, slug = "old-slug")
            val second = obs(number = 2, slug = "new-slug")

            // When
            val groups = group(first, second)

            // Then: a single group — grouping follows the identifier
            assertEquals(1, groups.size)
            assertEquals("new-slug", groups.single().animeSlug)
        }

        @Test
        fun `should treat two observations with the same coordinates as one episode`() {
            // Given: same (season, episodeType, number) but different
            // episodeIds — flat data duplicates must not split the episode
            val first = obs(episodeId = Uuid.random(), number = 1, audioLocale = Locale.JA_JP)
            val second = obs(episodeId = Uuid.random(), number = 1, audioLocale = Locale.FR_FR)

            // When
            val groups = group(first, second)

            // Then: one episode carrying both releases
            assertEquals(1, groups.size)
            assertEquals(1, groups.single().episodes.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should keep two releases of the same episode locale and instant`() {
            // Given: same episode, same locale, same release instant but
            // different release ids — two platform posts of the same track
            val episode = Uuid.random()
            val first = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)
            val second = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)

            // When
            val groups = group(first, second)

            // Then: both releases are kept
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should keep the earliest sighting when a release id is re-observed`() {
            // Given: the same releaseId seen twice with different dates —
            // the legacy indexer removed the old observation before
            // re-inserting; the port keeps the earliest deterministically
            val releaseId = Uuid.random()
            val episode = Uuid.random()
            val earlier = obs(episodeId = episode, number = 1, releaseId = releaseId, releaseDateTime = base)
            val later = obs(episodeId = episode, number = 1, releaseId = releaseId, releaseDateTime = base + 1.hours)

            // When
            val groups = group(later, earlier)

            // Then: a single release at the earliest date
            assertEquals(1, groups.size)
            assertEquals(1, groups.single().releases.size)
            assertEquals(base, groups.single().releaseDateTime)
        }

        @Test
        fun `should bucket every episode type independently`() {
            // Given: one observation per episode type at the same instant,
            // all on the same anime — only episodeType differs
            val observations = EpisodeType.entries.map { type ->
                obs(number = 1, episodeType = type)
            }

            // When
            val groups = group(*observations.toTypedArray())

            // Then: each type is its own bucket
            assertEquals(EpisodeType.entries.size, groups.size)
        }
    }

    @Nested
    @DisplayName("coverage signature split (NewGroupIndexer semantics)")
    inner class CoverageTests {

        @Test
        fun `should split a bucket when language coverages differ`() {
            // Given: ep1 = SUB+VOICE, ep2 = SUB only — same bucket.
            // SUB covers {1,2}, VOICE covers {1}: two distinct signatures.
            val ep1Sub = obs(number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base)
            val ep1Voice = obs(number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base)
            val ep2Sub = obs(number = 2, audioLocale = Locale.JA_JP, releaseDateTime = base)

            // When
            val groups = group(ep1Sub, ep1Voice, ep2Sub)

            // Then: the SUB observations merge on coverage {1,2}, the lone
            // VOICE observation forms its own group — ep1's voice is NOT
            // grouped with ep1's subtitles (legacy behaviour)
            assertEquals(2, groups.size)
            val subGroup = groups.single { it.releases.size == 2 }
            val voiceGroup = groups.single { it.releases.size == 1 }
            assertEquals(setOf(LangType.SUBTITLES), subGroup.coverage.keys)
            assertEquals(setOf(LangType.VOICE), voiceGroup.coverage.keys)
        }

        @Test
        fun `should date a split group by its own earliest release`() {
            // Given: bucket anchored at 0h; ep1 voice lands at +30min and
            // forms its own group — its date is +30min, not the anchor
            val ep1Sub = obs(number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base)
            val ep1Voice = obs(number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base + 30.minutes)
            val ep2Sub = obs(number = 2, audioLocale = Locale.JA_JP, releaseDateTime = base)

            // When
            val groups = group(ep1Sub, ep1Voice, ep2Sub)

            // Then
            val voiceGroup = groups.single { it.coverage.keys == setOf(LangType.VOICE) }
            assertEquals(base + 30.minutes, voiceGroup.releaseDateTime)
        }

        @Test
        fun `should merge SUB and VOICE observations when their coverages are identical`() {
            // Given: ep1 and ep2 both covered SUB+VOICE —
            // SUB covers {1,2} and VOICE covers {1,2}: same signature
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

            // Then: one single group mixing both lang types
            assertEquals(1, groups.size)
            assertEquals(setOf(LangType.SUBTITLES, LangType.VOICE), groups.single().coverage.keys)
            assertEquals(setOf(1, 2), groups.single().episodes.map { it.number }.toSet())
        }

        @Test
        fun `should handle crossed coverages across three episodes`() {
            // Given: ep1 SUB+VOICE, ep2 SUB only, ep3 VOICE only
            // SUB covers {1,2}, VOICE covers {1,3}
            val observations = listOf(
                obs(number = 1, audioLocale = Locale.JA_JP),
                obs(number = 1, audioLocale = Locale.FR_FR),
                obs(number = 2, audioLocale = Locale.JA_JP),
                obs(number = 3, audioLocale = Locale.FR_FR)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then: {ep1.sub, ep2.sub} and {ep1.voice, ep3.voice}
            assertEquals(2, groups.size)
            val subGroup = groups.single { it.coverage.keys == setOf(LangType.SUBTITLES) }
            val voiceGroup = groups.single { it.coverage.keys == setOf(LangType.VOICE) }
            assertEquals(setOf(1, 2), subGroup.episodes.map { it.number }.toSet())
            assertEquals(setOf(1, 3), voiceGroup.episodes.map { it.number }.toSet())
        }

        @Test
        fun `should compute the coverage signature per time bucket`() {
            // Given: ep1 SUB+VOICE at 0h, ep2 SUB-only at +5h — two separate
            // windows. Inside bucket 1, SUB and VOICE both cover {1} only,
            // so they must merge; an anime-wide signature would wrongly
            // split ep1 (SUB would cover {1,2}, VOICE only {1})
            val ep1Sub = obs(number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base)
            val ep1Voice = obs(number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base)
            val ep2Sub = obs(number = 2, audioLocale = Locale.JA_JP, releaseDateTime = base + 5.hours)

            // When
            val groups = group(ep1Sub, ep1Voice, ep2Sub)

            // Then: 2 groups — ep1 merged SUB+VOICE, ep2 alone
            assertEquals(2, groups.size)
            assertEquals(2, groups.maxOf { it.releases.size })
            assertEquals(1, groups.minOf { it.releases.size })
        }

        @Test
        fun `should merge every CJK subtitle locale under the same LangType`() {
            // Given: one episode with ja, zh, ko subtitles and a fr voice —
            // all CJK locales share the SUBTITLES lang type
            val episode = Uuid.random()
            val observations = listOf(
                obs(episodeId = episode, number = 1, audioLocale = Locale.JA_JP),
                obs(episodeId = episode, number = 1, audioLocale = Locale.ZH_CN),
                obs(episodeId = episode, number = 1, audioLocale = Locale.KO_KR),
                obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then: identical coverages {1} merge everything into one group
            assertEquals(1, groups.size)
            assertEquals(4, groups.single().releases.size)
        }

        @Test
        fun `should exclude releases whose locale is not visible for the country`() {
            // Given: for Country.FR the visible locales are CJK subtitles (or EN
            // fallback) plus fr-FR voice; a de-DE release must be excluded
            val episode = Uuid.random()
            val fr = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)
            val sub = obs(episodeId = episode, number = 1, audioLocale = Locale.JA_JP)
            val german = obs(episodeId = episode, number = 1, audioLocale = Locale.DE_DE)

            // When
            val groups = group(fr, sub, german, country = Country.FR)

            // Then: the group only carries the fr-FR and ja-JP releases
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should drop an episode whose locales are all invisible`() {
            // Given: ep1 covered SUB+VOICE, ep2 only de-DE — invisible for FR
            // it must neither form a group nor feed the SUB signature
            val observations = listOf(
                obs(number = 1, audioLocale = Locale.JA_JP),
                obs(number = 1, audioLocale = Locale.FR_FR),
                obs(number = 2, audioLocale = Locale.DE_DE)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then: one group on episode 1 only — SUB coverage stays {1}
            assertEquals(1, groups.size)
            assertEquals(setOf(1), groups.single().episodes.map { it.number }.toSet())
            assertEquals(setOf(1), groups.single().coverage[LangType.SUBTITLES]?.map { it.number }?.toSet())
        }

        @Test
        fun `should not let an invisible release anchor a bucket`() {
            // Given: an invisible de-DE release at the anchor instant would
            // pull ep1 (+1h) into its bucket and push ep2 (+2h30) out;
            // filtered out, ep1 anchors and ep2 stays inside the window
            val ghost = obs(number = 9, audioLocale = Locale.DE_DE, releaseDateTime = base)
            val ep1 = obs(number = 1, audioLocale = Locale.JA_JP, releaseDateTime = base + 1.hours)
            val ep2 = obs(number = 2, audioLocale = Locale.JA_JP, releaseDateTime = base + 2.hours + 30.minutes)

            // When
            val groups = group(ghost, ep1, ep2)

            // Then: a single group holding both visible episodes
            assertEquals(1, groups.size)
            assertEquals(setOf(1, 2), groups.single().episodes.map { it.number }.toSet())
        }

        @Test
        fun `should fall back to english subtitles when no CJK locale is available`() {
            // Given: an episode with en-US and fr-FR releases only
            val episode = Uuid.random()
            val en = obs(episodeId = episode, number = 1, audioLocale = Locale.EN_US)
            val fr = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)

            // When
            val groups = group(en, fr)

            // Then: en-US counts as SUBTITLES, the group exists
            assertEquals(1, groups.size)
            assertTrue(groups.single().coverage.containsKey(LangType.SUBTITLES))
        }

        @Test
        fun `should not use english subtitles when a CJK locale is available`() {
            // Given: en-US + ja-JP + fr-FR on the same episode —
            // ja-JP wins the subtitle slot, en-US is excluded for FR
            val episode = Uuid.random()
            val en = obs(episodeId = episode, number = 1, audioLocale = Locale.EN_US)
            val ja = obs(episodeId = episode, number = 1, audioLocale = Locale.JA_JP)
            val fr = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)

            // When
            val groups = group(en, ja, fr)

            // Then: only ja-JP (SUB) + fr-FR (VOICE) remain
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should keep only the releases matching searchTypes`() {
            // Given: ep1 SUB+VOICE and ep2 SUB-only share a bucket —
            // under NewGroupIndexer the voice already sits in its own group
            val observations = listOf(
                obs(number = 1, audioLocale = Locale.JA_JP),
                obs(number = 1, audioLocale = Locale.FR_FR),
                obs(number = 2, audioLocale = Locale.JA_JP)
            )

            // When: only VOICE releases are requested
            val groups = group(*observations.toTypedArray(), searchTypes = setOf(LangType.VOICE))

            // Then: a single group holding only the voice release — the
            // subtitle-only group is dropped and matching releases are kept
            assertEquals(1, groups.size)
            assertEquals(1, groups.single().releases.size)
        }

        @Test
        fun `should treat an empty searchTypes set as no filter`() {
            // Given: the legacy endpoint turned an empty array into "match
            // nothing"; the port treats it like a missing filter
            val episode = Uuid.random()
            val observations = listOf(
                obs(episodeId = episode, number = 1, audioLocale = Locale.JA_JP),
                obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR)
            )

            // When
            val groups = group(*observations.toTypedArray(), searchTypes = emptySet())

            // Then
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }
    }

    @Nested
    @DisplayName("group content")
    inner class ContentTests {

        @Test
        fun `should expose the earliest release date of the group`() {
            // Given: releases at +0h and +1h
            val first = obs(number = 1, releaseDateTime = base)
            val second = obs(number = 1, releaseDateTime = base + 1.hours)

            // When
            val group = group(first, second).single()

            // Then: the group date is its earliest member release
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
        fun `should keep two releases of the same episode and locale at different dates`() {
            // Given: same episode, same locale, two platform dates inside
            // the window — the legacy indexer kept both variants
            val episode = Uuid.random()
            val first = obs(episodeId = episode, number = 1, audioLocale = Locale.FR_FR, releaseDateTime = base)
            val second = obs(
                episodeId = episode, number = 1, audioLocale = Locale.FR_FR,
                releaseDateTime = base + 30.minutes
            )

            // When
            val groups = group(first, second)

            // Then
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().releases.size)
        }

        @Test
        fun `should expose season and number ranges across a multi-episode group`() {
            // Given: a batch release spanning two seasons
            val observations = listOf(
                obs(season = 1, number = 12, audioLocale = Locale.JA_JP),
                obs(season = 1, number = 12, audioLocale = Locale.FR_FR),
                obs(season = 2, number = 1, audioLocale = Locale.JA_JP),
                obs(season = 2, number = 1, audioLocale = Locale.FR_FR)
            )

            // When
            val group = group(*observations.toTypedArray()).single()

            // Then: the group aggregates both seasons and numbers
            assertEquals(1, group.minSeason)
            assertEquals(2, group.maxSeason)
            assertEquals(1, group.minNumber)
            assertEquals(12, group.maxNumber)
        }

        @Test
        fun `should order groups by release date, slug, type, season and number`() {
            // Given: groups on different dates and animes
            val later = obs(number = 1, releaseDateTime = base + 1.days, slug = "b-anime", anime = Uuid.random())
            val earlier = obs(number = 1, releaseDateTime = base, slug = "a-anime", anime = Uuid.random())
            val sameDate = obs(number = 1, releaseDateTime = base, slug = "z-anime", anime = Uuid.random())

            // When
            val groups = group(later, earlier, sameDate)

            // Then: ascending release date first, slug as tiebreak
            assertEquals(listOf("a-anime", "z-anime", "b-anime"), groups.map { it.animeSlug })
        }

        @Test
        fun `should order same-date groups by episode type then season and number`() {
            // Given: same anime, same instant — an EPISODE group covering
            // season 2 number 1 and a FILM group
            val shared = Uuid.random()
            val observations = listOf(
                obs(anime = shared, season = 2, number = 1, audioLocale = Locale.JA_JP),
                obs(anime = shared, season = 2, number = 1, audioLocale = Locale.FR_FR),
                obs(anime = shared, number = 5, audioLocale = Locale.JA_JP, episodeType = EpisodeType.FILM)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then: EPISODE is declared before FILM in the enum, so the
            // episode group sorts first on the episodeType key
            assertEquals(EpisodeType.EPISODE, groups.first().episodeType)
            assertEquals(EpisodeType.FILM, groups.last().episodeType)
        }

        @Test
        fun `should break full ties by coverage signature then release id`() {
            // Given: ep1 SUB+VOICE and ep2 SUB-only produce two groups that
            // tie on date, slug, type, season and number — only the coverage
            // signature differs
            val observations = listOf(
                obs(number = 1, audioLocale = Locale.JA_JP),
                obs(number = 1, audioLocale = Locale.FR_FR),
                obs(number = 2, audioLocale = Locale.JA_JP)
            )

            // When
            val groups = group(*observations.toTypedArray())

            // Then: deterministic order — SUBTITLES signature before VOICE
            assertEquals(
                listOf(setOf(LangType.SUBTITLES), setOf(LangType.VOICE)),
                groups.map { it.coverage.keys }
            )
        }
    }
}
