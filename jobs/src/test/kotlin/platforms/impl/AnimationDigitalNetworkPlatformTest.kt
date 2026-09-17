package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.models.Platform
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
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@DisplayName("tests for AnimationDigitalNetworkPlatform")
class AnimationDigitalNetworkPlatformTest {

    private fun smartHttpClient(bodyProvider: () -> String): SmartHttpClient =
        SmartHttpClient(
            client = HttpClient(MockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }

                engine {
                    addHandler {
                        respond(
                            bodyProvider(),
                            HttpStatusCode.OK,
                            headersOf("Content-Type", "application/json")
                        )
                    }
                }
            },
            maxStaleRetention = 1.minutes
        )

    private fun calendarBody(firstId: Int = 1001, secondTitle: String = "Autre - Épisode 3"): String = """
        {
          "videos": [
            {
              "id": $firstId,
              "title": "Major - Episode 105",
              "name": "Major 105",
              "shortNumber": "105",
              "season": "5",
              "type": "EPS",
              "releaseDate": "2026-01-10T08:00:00Z",
              "show": { "id": 1353, "title": "Major" }
            },
            {
              "id": 1002,
              "title": "$secondTitle",
              "name": "Autre 3",
              "shortNumber": "3",
              "season": "1",
              "type": "EPS",
              "releaseDate": "2026-06-15T14:30:00+02:00",
              "show": { "id": 42, "title": "Autre" }
            }
          ]
        }
    """.trimIndent()

    @Nested
    @DisplayName("tests for the 'fetchLatestEpisodes' method")
    inner class FetchLatestEpisodesTests {

        @Test
        @DisplayName("should map every video of the calendar to a platform episode with its release date")
        fun `should map every video of the calendar to a platform episode with its release date`() = runTest {
            // Given
            val platform = AnimationDigitalNetworkPlatform(smartHttpClient { calendarBody() })

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals("1001", episodes[0].id)
            assertEquals("Major - Episode 105", episodes[0].title)
            assertEquals("1002", episodes[1].id)
            assertEquals("Autre - Épisode 3", episodes[1].title)
        }

        @Test
        @DisplayName("should reuse the same mapped list instance while the calendar content is unchanged")
        fun `should reuse the same mapped list instance while the calendar content is unchanged`() = runTest {
            // Given
            var body = calendarBody()
            val platform = AnimationDigitalNetworkPlatform(
                smartHttpClient { body },
                calendarTtl = Duration.ZERO
            )

            // When
            val first = platform.fetchLatestEpisodes()
            val second = platform.fetchLatestEpisodes()

            // Then
            assertSame(first, second)
            assertTrue(first.isNotEmpty())
        }

        @Test
        @DisplayName("should convert again when the calendar content changes")
        fun `should convert again when the calendar content changes`() = runTest {
            // Given
            var body = calendarBody()
            val platform = AnimationDigitalNetworkPlatform(
                smartHttpClient { body },
                calendarTtl = Duration.ZERO
            )
            val first = platform.fetchLatestEpisodes()

            // When
            body = calendarBody(firstId = 2002)
            val second = platform.fetchLatestEpisodes()

            // Then
            assertTrue(first !== second)
            assertEquals("2002", second[0].id)
        }

        @Test
        @DisplayName("should return an empty list when the calendar has no videos")
        fun `should return an empty list when the calendar has no videos`() = runTest {
            // Given
            val platform = AnimationDigitalNetworkPlatform(smartHttpClient { """{"videos": []}""" })

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(0, episodes.size)
        }
    }
}
