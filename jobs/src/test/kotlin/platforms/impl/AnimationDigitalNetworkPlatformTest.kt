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
import kotlin.time.Duration.Companion.minutes

@DisplayName("tests for AnimationDigitalNetworkPlatform")
class AnimationDigitalNetworkPlatformTest {

    private fun mockSmartHttpClient(body: String): SmartHttpClient =
        SmartHttpClient(
            client = HttpClient(MockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }

                engine {
                    addHandler {
                        respond(
                            body,
                            HttpStatusCode.OK,
                            headersOf("Content-Type", "application/json")
                        )
                    }
                }
            }
        )


    private fun calendarBody(): String = """
        {
          "videos": [
            {
              "id": 1001,
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
              "title": "Autre - Épisode 3",
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
    @DisplayName("tests for the 'fetchEpisodes' method")
    inner class FetchEpisodesTests {

        @Test
        @DisplayName("should map every video of the calendar to a platform episode with its release date")
        fun `should map every video of the calendar to a platform episode with its release date`() = runTest {
            // Given
            val platform = AnimationDigitalNetworkPlatform(mockSmartHttpClient(calendarBody()))

            // When
            val episodes = platform.fetchEpisodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals("1001", episodes[0].id)
            assertEquals("Major - Episode 105", episodes[0].title)
            assertEquals(ZonedDateTime.parse("2026-01-10T08:00:00Z"), episodes[0].releaseDateTime)
            assertEquals("1002", episodes[1].id)
            assertEquals(ZonedDateTime.parse("2026-06-15T14:30:00+02:00"), episodes[1].releaseDateTime)
        }

        @Test
        @DisplayName("should return an empty list when the calendar has no videos")
        fun `should return an empty list when the calendar has no videos`() = runTest {
            // Given
            val platform = AnimationDigitalNetworkPlatform(mockSmartHttpClient("""{"videos": []}"""))

            // When
            val episodes = platform.fetchEpisodes()

            // Then
            assertEquals(0, episodes.size)
        }

        @Test
        @DisplayName("should expose the ADN platform")
        fun `should expose the ADN platform`() {
            // Given
            val platform = AnimationDigitalNetworkPlatform(mockSmartHttpClient(calendarBody()))

            // When
            val exposed = platform.platform

            // Then
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, exposed)
        }
    }
}
