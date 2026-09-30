package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Platform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("tests for the ingestion diagnosis vocabulary")
class IngestionVerdictTest {

    private fun item(
        platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK,
        id: String = "1001",
        showTitle: String = "One Piece",
        title: String = "Episode 1",
        releaseDateTime: String? = "2026-01-10T08:00:00Z",
        audioLocales: List<String> = listOf("vostf")
    ) = PlatformItem(
        platform = platform,
        platformId = id,
        showTitle = showTitle,
        title = title,
        releaseDateTime = releaseDateTime,
        audioLocales = audioLocales
    )

    @Nested
    @DisplayName("tests for the rejection reason taxonomy")
    inner class RejectionReasonTests {

        @Test
        @DisplayName("should attach the platform stage to every content filter reason")
        fun `should attach the platform stage to every content filter reason`() {
            // Given / When
            val platformReasons = RejectionReason.entries.filter { it.stage == RejectionStage.PLATFORM }

            // Then
            assertEquals(
                setOf(
                    RejectionReason.PROMOTIONAL_CONTENT,
                    RejectionReason.TRAILER_OR_OPENING,
                    RejectionReason.NOT_AN_ANIMATION,
                    RejectionReason.EMPTY_GENRES
                ),
                platformReasons.toSet()
            )
        }

        @Test
        @DisplayName("should cover every ingestion stage with at least one reason")
        fun `should cover every ingestion stage with at least one reason`() {
            // Given
            val stages = RejectionStage.entries

            // When
            val covered = RejectionReason.entries.map { it.stage }.toSet()

            // Then
            assertEquals(stages.toSet(), covered, "every stage needs at least one reason, otherwise a rejection cannot be categorised")
        }
    }

    @Nested
    @DisplayName("tests for the verdict construction")
    inner class VerdictTests {

        @Test
        @DisplayName("should keep the item and the reason on a rejection")
        fun `should keep the item and the reason on a rejection`() {
            // Given
            val rejected = item()

            // When
            val verdict = IngestionVerdict.rejected(
                item = rejected,
                reason = RejectionReason.NOT_AN_ANIMATION,
                evidence = "genres=[Live Action, Drama]"
            )

            // Then
            assertTrue(verdict is IngestionVerdict.Rejected)
            assertEquals(rejected, verdict.item)
            assertEquals(RejectionReason.NOT_AN_ANIMATION, verdict.reason)
            assertEquals("genres=[Live Action, Drama]", verdict.evidence)
        }

        @Test
        @DisplayName("should refuse a rejection without evidence")
        fun `should refuse a rejection without evidence`() {
            // Given / When / Then an evidence-less rejection is undebuggable, so it must not build
            assertFailsWith<IllegalArgumentException> {
                IngestionVerdict.rejected(
                    item = item(),
                    reason = RejectionReason.NOT_AN_ANIMATION,
                    evidence = "   "
                )
            }
        }

        @Test
        @DisplayName("should accept a verdict carrying an accepted episode")
        fun `should accept a verdict carrying an accepted episode`() {
            // Given
            val episode = PlatformEpisode(
                anime = PlatformAnime(id = "1", title = "One Piece", description = null, thumbnail = "thumb"),
                id = "1001",
                title = "Episode 1",
                description = null,
                image = "image",
                releaseDateTime = ZonedDateTime.parse("2026-01-10T08:00:00Z")
            )

            // When
            val verdict = IngestionVerdict.accepted(item = item(), episode = episode)

            // Then
            assertTrue(verdict is IngestionVerdict.Accepted)
            assertEquals(episode, verdict.episode)
        }
    }

    @Nested
    @DisplayName("tests for the platform item projection")
    inner class PlatformItemTests {

        @Test
        @DisplayName("should render a readable one line summary")
        fun `should render a readable one line summary`() {
            // Given
            val item = item()

            // When
            val description = item.describe()

            // Then
            assertEquals("ANIMATION_DIGITAL_NETWORK #1001 — One Piece / Episode 1 (vostf, 2026-01-10T08:00:00Z)", description)
        }

        @Test
        @DisplayName("should tolerate a missing release date")
        fun `should tolerate a missing release date`() {
            // Given
            val item = item(releaseDateTime = null, audioLocales = emptyList())

            // When
            val description = item.describe()

            // Then
            assertTrue(description.contains("no release date"), "expected an explicit marker, got: $description")
        }
    }
}
