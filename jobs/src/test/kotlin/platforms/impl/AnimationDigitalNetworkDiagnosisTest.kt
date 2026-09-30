package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.diagnostics.IngestionVerdict
import fr.shikkanime.jobs.diagnostics.RejectionReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration

@DisplayName("tests for the ADN ingestion diagnosis")
class AnimationDigitalNetworkDiagnosisTest {

    private fun smartHttpClient(body: String): SmartHttpClient =
        SmartHttpClient(
            client = HttpClient(
                MockEngine {
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json")
                    )
                }
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        )

    private fun video(
        id: Int,
        name: String = "Episode $id",
        shortNumber: String = "$id",
        type: String = "EPS",
        showId: Int = 1,
        showTitle: String = "One Piece",
        showGenres: List<String> = listOf("Animation japonaise", "Action")
    ): String = """
        {
          "id": $id,
          "name": "$name",
          "shortNumber": "$shortNumber",
          "type": "$type",
          "image2x": "https://image.animationdigitalnetwork.com/video/$id/100x100/eps",
          "summary": "Summary $id",
          "releaseDate": "2026-01-10T08:00:00Z",
          "show": {
            "id": $showId,
            "title": "$showTitle",
            "summary": "Show summary",
            "image2x": "https://image.animationdigitalnetwork.com/show/$showId/100x100/portrait-with-logo",
            "genres": [${showGenres.joinToString(", ") { "\"$it\"" }}]
          }
        }
    """.trimIndent()

    private fun calendar(vararg videos: String): String = """{"videos": [${videos.joinToString(",")}]}"""

    private suspend fun diagnose(body: String) = AnimationDigitalNetworkPlatform(
        client = smartHttpClient(body),
        ttl = Duration.ZERO
    ).diagnoseLatestEpisodes()

    @Nested
    @DisplayName("tests for the verdicts produced from a calendar")
    inner class VerdictsTests {

        @Test
        @DisplayName("should accept a regular broadcast episode")
        fun `should accept a regular broadcast episode`() = runTest {
            // Given
            val run = diagnose(calendar(video(1001, name = "One Piece 105", shortNumber = "105")))

            // Then
            val accepted = assertIs<IngestionVerdict.Accepted>(run.verdicts.single())
            assertEquals("1001", accepted.episode.id)
            assertEquals("One Piece 105", accepted.episode.title)
        }

        @Test
        @DisplayName("should reject a promotional video with its type as evidence")
        fun `should reject a promotional video with its type as evidence`() = runTest {
            // Given
            val run = diagnose(calendar(video(1, type = "PV", shortNumber = "Teaser"), video(2, type = "BONUS")))

            // Then
            val rejected = run.verdicts.map { assertIs<IngestionVerdict.Rejected>(it) }
            assertEquals(listOf("1", "2"), rejected.map { it.item.platformId })
            assertTrue(rejected.all { it.reason == RejectionReason.PROMOTIONAL_CONTENT })
            assertTrue(rejected.first().evidence.contains("PV"), "expected the type in the evidence, got: ${rejected.first().evidence}")
            assertTrue(rejected[1].evidence.contains("BONUS"), "expected the type in the evidence, got: ${rejected[1].evidence}")
        }

        @Test
        @DisplayName("should reject a trailer with its short number as evidence")
        fun `should reject a trailer with its short number as evidence`() = runTest {
            // Given
            val run = diagnose(calendar(video(1, shortNumber = "Bande-annonce 1"), video(2, shortNumber = "Making-of 1")))

            // Then
            val rejected = run.verdicts.map { assertIs<IngestionVerdict.Rejected>(it) }
            assertTrue(rejected.all { it.reason == RejectionReason.TRAILER_OR_OPENING })
            assertTrue(rejected.first().evidence.contains("Bande-annonce 1"), "got: ${rejected.first().evidence}")
        }

        @Test
        @DisplayName("should reject a show without the animation genre with its genres as evidence")
        fun `should reject a show without the animation genre with its genres as evidence`() = runTest {
            // Given
            val run = diagnose(calendar(video(1, showId = 10, showGenres = listOf("Live Action", "Drama"))))

            // Then
            val rejected = assertIs<IngestionVerdict.Rejected>(run.verdicts.single())
            assertEquals(RejectionReason.NOT_AN_ANIMATION, rejected.reason)
            assertTrue(rejected.evidence.contains("Live Action"), "got: ${rejected.evidence}")
        }

        @Test
        @DisplayName("should distinguish a show with an empty genre list from a non animation show")
        fun `should distinguish a show with an empty genre list from a non animation show`() = runTest {
            // Given
            val run = diagnose(calendar(video(1, showId = 10, showGenres = emptyList())))

            // Then
            val rejected = assertIs<IngestionVerdict.Rejected>(run.verdicts.single())
            assertEquals(RejectionReason.EMPTY_GENRES, rejected.reason)
        }

        @Test
        @DisplayName("should produce one verdict per video of the calendar")
        fun `should produce one verdict per video of the calendar`() = runTest {
            // Given
            val run = diagnose(calendar(video(1), video(2, type = "PV"), video(3, showId = 10, showGenres = listOf("Drama"))))

            // Then
            assertEquals(3, run.verdicts.size)
            assertEquals(1, run.acceptedCount)
            assertEquals(2, run.rejectedCount)
        }

        @Test
        @DisplayName("should reject every video of a show already known to be invalid")
        fun `should reject every video of a show already known to be invalid`() = runTest {
            // Given two videos of the same non animation show, which exercises the memoized invalidShowIds path
            val run = diagnose(calendar(video(1, showId = 10, showGenres = listOf("Drama")), video(2, showId = 10, showGenres = listOf("Drama"))))

            // Then both videos must carry a verdict, not just the first one
            val rejected = run.verdicts.map { assertIs<IngestionVerdict.Rejected>(it) }
            assertEquals(2, rejected.size)
            assertTrue(rejected.all { it.reason == RejectionReason.NOT_AN_ANIMATION })
        }
    }

    @Nested
    @DisplayName("tests for the diagnosis run metadata")
    inner class RunMetadataTests {

        @Test
        @DisplayName("should report the platform and a fetch timestamp")
        fun `should report the platform and a fetch timestamp`() = runTest {
            // Given
            val run = diagnose(calendar(video(1)))

            // Then
            assertEquals(null, run.error, "the calendar should have been parsed, error was: ${run.error}")
            assertEquals(fr.shikkanime.models.Platform.ANIMATION_DIGITAL_NETWORK, run.platform)
            assertEquals(1, run.totalCount)
        }

        @Test
        @DisplayName("should still produce a run when the platform call fails")
        fun `should still produce a run when the platform call fails`() = runTest {
            // Given a calendar the client cannot parse, so the fetch fails
            val platform = AnimationDigitalNetworkPlatform(
                client = smartHttpClient("""{"videos": "not-an-array"}"""),
                ttl = Duration.ZERO
            )

            // When
            val run = platform.diagnoseLatestEpisodes()

            // Then the failure is visible instead of the registry staying empty
            assertTrue(run.error != null, "expected the fetch error to be reported, got: ${run.error}")
            assertEquals(0, run.totalCount)
        }

        @Test
        @DisplayName("should keep propagating a failing fetch from the ingestion path")
        fun `should keep propagating a failing fetch from the ingestion path`() = runTest {
            // Given the same failing client on the ingestion path
            val platform = AnimationDigitalNetworkPlatform(
                client = smartHttpClient("""{"videos": "not-an-array"}"""),
                ttl = Duration.ZERO
            )

            // When / Then swallowing the failure here would serve stale episodes as fresh ones
            assertFailsWith<Exception> { platform.fetchLatestEpisodes() }
        }
    }
}
