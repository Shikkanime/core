package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Platform
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("tests for the ingestion run registry")
class IngestionRunRegistryTest {

    private val fetchedAt = LocalDateTime(2026, 1, 10, 8, 0, 0)

    private fun item(platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK, id: String = "1") =
PlatformItem(
        platform = platform,
        platformId = id,
        showTitle = "One Piece",
        title = "Episode $id",
        releaseDateTime = "2026-01-10T08:00:00Z",
        audioLocales = listOf("vostf")
    )

    private fun episode(id: String = "1") =
PlatformEpisode(
        anime = PlatformAnime(id = "1", title = "One Piece", description = null, thumbnail = "thumb"),
        id = id,
        title = "Episode $id",
        description = null,
        image = "image",
        releaseDateTime = ZonedDateTime.parse("2026-01-10T08:00:00Z")
    )

    private fun run(
        platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK,
        verdicts: List<IngestionVerdict> = listOf(IngestionVerdict.accepted(item(platform), episode())),
        error: String? = null
    ) = IngestionRun(platform = platform, verdicts = verdicts, fetchedAt = fetchedAt, error = error)

    @Nested
    @DisplayName("tests for the retention of the last run")
    inner class RetentionTests {

        @Test
        @DisplayName("should keep the last run of a platform")
        fun `should keep the last run of a platform`() {
            // Given
            val registry = IngestionRunRegistry()
            val recorded = run()
            registry.record(recorded)

            // When
            val latest = registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK)

            // Then
            assertEquals(recorded, latest)
        }

        @Test
        @DisplayName("should replace the previous run of the same platform")
        fun `should replace the previous run of the same platform`() {
            // Given
            val registry = IngestionRunRegistry()
            val first = run(verdicts = listOf(IngestionVerdict.accepted(item(id = "1"), episode("1"))))
            val second = run(verdicts = listOf(IngestionVerdict.accepted(item(id = "2"), episode("2"))))
            registry.record(first)

            // When
            registry.record(second)

            // Then only the latest run survives, this is the whole retention contract
            assertEquals(second, registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK))
            assertEquals(1, registry.latestRuns().size)
        }

        @Test
        @DisplayName("should keep one run per distinct platform")
        fun `should keep one run per distinct platform`() {
            // Given
            val registry = IngestionRunRegistry()
            registry.record(run(Platform.ANIMATION_DIGITAL_NETWORK))
            registry.record(run(Platform.CRUNCHYROLL))

            // When
            val latest = registry.latestRuns()

            // Then
            assertEquals(2, latest.size)
            assertEquals(
                setOf(Platform.ANIMATION_DIGITAL_NETWORK, Platform.CRUNCHYROLL),
                latest.map { it.platform }.toSet()
            )
        }

        @Test
        @DisplayName("should return null for a platform that was never recorded")
        fun `should return null for a platform that was never recorded`() {
            // Given
            val registry = IngestionRunRegistry()

            // When / Then
            assertNull(registry.latestRun(Platform.NETFLIX))
            assertEquals(0, registry.latestRuns().size)
        }

        @Test
        @DisplayName("should keep a run that failed so the failure stays visible")
        fun `should keep a run that failed so the failure stays visible`() {
            // Given
            val registry = IngestionRunRegistry()
            val failed = run(verdicts = emptyList(), error = "HTTP 500")

            // When
            registry.record(failed)

            // Then
            assertEquals("HTTP 500", registry.latestRun(Platform.ANIMATION_DIGITAL_NETWORK)?.error)
        }
    }

    @Nested
    @DisplayName("tests for the report rendering")
    inner class ReportTests {

        @Test
        @DisplayName("should count accepted rejected and total items")
        fun `should count accepted rejected and total items`() {
            // Given
            val run = run(
                verdicts = listOf(
                    IngestionVerdict.accepted(item(id = "1"), episode("1")),
                    IngestionVerdict.accepted(item(id = "2"), episode("2")),
                    IngestionVerdict.rejected(item(id = "3"), RejectionReason.PROMOTIONAL_CONTENT, "type=PV"),
                    IngestionVerdict.rejected(item(id = "4"), RejectionReason.NOT_AN_ANIMATION, "genres=[Drama]")
                )
            )

            // Then
            assertEquals(4, run.totalCount)
            assertEquals(2, run.acceptedCount)
            assertEquals(2, run.rejectedCount)
        }

        @Test
        @DisplayName("should group rejections by reason sorted by frequency")
        fun `should group rejections by reason sorted by frequency`() {
            // Given
            val run = run(
                verdicts = listOf(
                    IngestionVerdict.rejected(item(id = "1"), RejectionReason.NOT_AN_ANIMATION, "genres=[Drama]"),
                    IngestionVerdict.rejected(item(id = "2"), RejectionReason.PROMOTIONAL_CONTENT, "type=PV"),
                    IngestionVerdict.rejected(item(id = "3"), RejectionReason.PROMOTIONAL_CONTENT, "type=BONUS"),
                    IngestionVerdict.rejected(item(id = "4"), RejectionReason.PROMOTIONAL_CONTENT, "type=PV")
                )
            )

            // When
            val breakdown = run.rejectionBreakdown()

            // Then the most frequent reason comes first
            assertEquals(
                listOf(RejectionReason.PROMOTIONAL_CONTENT to 3, RejectionReason.NOT_AN_ANIMATION to 1),
                breakdown
            )
        }

        @Test
        @DisplayName("should render a one screen report naming every rejected item")
        fun `should render a one screen report naming every rejected item`() = runTest {
            // Given
            val run = run(
                verdicts = listOf(
                    IngestionVerdict.accepted(item(id = "1"), episode("1")),
                    IngestionVerdict.rejected(item(id = "2"), RejectionReason.NOT_AN_ANIMATION, "genres=[Live Action, Drama]"),
                    IngestionVerdict.rejected(item(id = "3"), RejectionReason.PROMOTIONAL_CONTENT, "type=PV")
                )
            )

            // When
            val report = run.describe()

            // Then each rejection must name the item, a reason alone is not enough to act on
            assertTrue(report.contains("3 item(s), 1 accepted, 2 rejected"), "got: $report")
            assertTrue(report.contains("NOT_AN_ANIMATION — ANIMATION_DIGITAL_NETWORK #2"), "the item must be named, got: $report")
            assertTrue(report.contains("genres=[Live Action, Drama]"), "the evidence must be shown, got: $report")
            assertTrue(report.contains("PROMOTIONAL_CONTENT — ANIMATION_DIGITAL_NETWORK #3"), "got: $report")
        }

        @Test
        @DisplayName("should report a fetch failure instead of an empty list")
        fun `should report a fetch failure instead of an empty list`() {
            // Given
            val run = run(verdicts = emptyList(), error = "HTTP 503: Service Unavailable")

            // When
            val report = run.describe()

            // Then
            assertTrue(report.contains("fetch failed"), "got: $report")
            assertTrue(report.contains("HTTP 503"), "got: $report")
        }

        @Test
        @DisplayName("should return the rejections of a single reason")
        fun `should return the rejections of a single reason`() {
            // Given
            val run = run(
                verdicts = listOf(
                    IngestionVerdict.rejected(item(id = "1"), RejectionReason.PROMOTIONAL_CONTENT, "type=PV"),
                    IngestionVerdict.rejected(item(id = "2"), RejectionReason.PROMOTIONAL_CONTENT, "type=BONUS"),
                    IngestionVerdict.rejected(item(id = "3"), RejectionReason.NOT_AN_ANIMATION, "genres=[Drama]")
                )
            )

            // When
            val promotional = run.rejectionsFor(RejectionReason.PROMOTIONAL_CONTENT)

            // Then
            assertEquals(listOf("1", "2"), promotional.map { it.item.platformId })
        }
    }
}
