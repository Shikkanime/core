package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Platform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@DisplayName("tests for AnimationDigitalNetworkPlatform")
class AnimationDigitalNetworkPlatformTest {

    private fun smartHttpClient(
        bodyProvider: () -> String,
        onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {}
    ): SmartHttpClient =
        SmartHttpClient(
            client = HttpClient(
                MockEngine { request ->
                    onRequest(request)
                    respond(
                        content = bodyProvider(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        )

    private fun smartHttpClient(body: String): SmartHttpClient =
smartHttpClient({ body })

    private fun platform(client: SmartHttpClient): AnimationDigitalNetworkPlatform =
        AnimationDigitalNetworkPlatform(client = client, ttl = Duration.ZERO)

    private fun video(
        id: Int,
        name: String = "Episode $id",
        shortNumber: String = "$id",
        type: AdnVideoType = AdnVideoType.EPS,
        summary: String? = "Summary $id",
        releaseDate: String = "2026-01-10T08:00:00Z",
        showId: Int = 1,
        showTitle: String = "One Piece",
        showSummary: String? = "Pirates summary",
        showGenres: List<String> = listOf("Animation japonaise", "Action")
    ): String = """
        {
          "id": $id,
          "name": "$name",
          "shortNumber": "$shortNumber",
          "type": "${type.name}",
          "image2x": "https://image.animationdigitalnetwork.com/video/$id/100x100/eps",
          "summary": ${summary?.let { "\"$it\"" } ?: "null"},
          "releaseDate": "$releaseDate",
          "show": {
            "id": $showId,
            "title": "$showTitle",
            "summary": ${showSummary?.let { "\"$it\"" } ?: "null"},
            "image2x": "https://image.animationdigitalnetwork.com/show/$showId/100x100/portrait-with-logo",
            "genres": [${showGenres.joinToString(", ") { "\"$it\"" }}]
          }
        }
    """.trimIndent()

    private fun calendar(vararg videos: String): String =
"""{"videos": [${videos.joinToString(",")}]}"""

    @Nested
    @DisplayName("tests for the 'fetchLatestEpisodes' method")
    inner class FetchLatestEpisodesTests {

        @Test
        @DisplayName("should map every video of the calendar to a platform episode")
        fun `should map every video of the calendar to a platform episode`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001, name = "Major 105", shortNumber = "105"), video(1002, name = "Autre 3", shortNumber = "3"))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals("1001", episodes[0].id)
            assertEquals("Major 105", episodes[0].title)
            assertEquals("1002", episodes[1].id)
            assertEquals("Autre 3", episodes[1].title)
        }

        @Test
        @DisplayName("should return an empty list when the calendar has no videos")
        fun `should return an empty list when the calendar has no videos`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar()))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(0, episodes.size)
        }

        @Test
        @DisplayName("should expose the animation digital network platform")
        fun `should expose the animation digital network platform`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar()))

            // When
            val actual = platform.platform

            // Then
            assertEquals(Platform.ANIMATION_DIGITAL_NETWORK, actual)
        }

        @Test
        @DisplayName("should map the video release date as the platform episode release date time")
        fun `should map the video release date as the platform episode release date time`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001, releaseDate = "2026-01-10T08:00:00Z"), video(1002, releaseDate = "2026-06-15T16:30:00Z"))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals("2026-01-10T08:00:00Z", episodes[0].releaseDateTime.toInstant().toString())
            assertEquals("2026-06-15T16:30:00Z", episodes[1].releaseDateTime.toInstant().toString())
        }

        @Test
        @DisplayName("should map the video summary and image to the platform episode")
        fun `should map the video summary and image to the platform episode`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001, summary = "A great episode"))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals("A great episode", episodes.single().description)
            assertEquals(
                "https://image.animationdigitalnetwork.com/video/1001/1920x1080/eps.width=1920,height=1080,quality=100",
                episodes.single().image
            )
        }

        @Test
        @DisplayName("should map the show as the platform anime of the episode")
        fun `should map the show as the platform anime of the episode`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001, showId = 1353, showTitle = "Major", showSummary = "A baseball story\nwith two lines"))))

            // When
            val anime = platform.fetchLatestEpisodes().single().anime

            // Then
            assertEquals("1353", anime.id)
            assertEquals("Major", anime.title)
            assertEquals("A baseball storywith two lines", anime.description)
        }

        @Test
        @DisplayName("should reuse the same mapped list instance while the calendar content is unchanged")
        fun `should reuse the same mapped list instance while the calendar content is unchanged`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001))))

            // When
            val first = platform.fetchLatestEpisodes()
            val second = platform.fetchLatestEpisodes()

            // Then
            assertSame(first, second)
        }

        @Test
        @DisplayName("should map again when the calendar content changes")
        fun `should map again when the calendar content changes`() = runTest {
            // Given
            var body = calendar(video(1001))
            val platform = platform(smartHttpClient({ body }))
            val first = platform.fetchLatestEpisodes()

            // When
            body = calendar(video(2002))
            val second = platform.fetchLatestEpisodes()

            // Then
            assertTrue(first !== second)
            assertEquals("2002", second.single().id)
        }

        @Test
        @DisplayName("should send the french distribution headers to the calendar endpoint")
        fun `should send the french distribution headers to the calendar endpoint`() = runTest {
            // Given
            val headers = mutableMapOf<String, String>()
            var requestedUrl: String? = null
            val platform = platform(
                smartHttpClient(
                    bodyProvider = { calendar(video(1001)) },
                    onRequest = { request ->
                        requestedUrl = request.url.toString()
                        request.headers.forEach { key, values -> headers[key] = values.joinToString(",") }
                    }
                )
            )

            // When
            platform.fetchLatestEpisodes()

            // Then
            assertEquals("fr", headers[HttpHeaders.AcceptLanguage])
            assertEquals("Web", headers["X-Source"])
            assertEquals("fr", headers["X-Target-Distribution"])
            assertTrue(requestedUrl!!.startsWith("https://gw.api.animationdigitalnetwork.com/video/calendar?date="), "unexpected url: $requestedUrl")
        }
    }

    @Nested
    @DisplayName("tests for the video filtering")
    inner class VideoFilteringTests {

        @Test
        @DisplayName("should exclude promotional videos")
        fun `should exclude promotional videos`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, type = AdnVideoType.EPS), video(2, type = AdnVideoType.PV), video(3, type = AdnVideoType.BONUS))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(listOf("1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should exclude trailers openings and making of videos")
        fun `should exclude trailers openings and making of videos`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1, shortNumber = "1"),
                        video(2, shortNumber = "Bande-annonce 1"),
                        video(3, shortNumber = "Bande annonce 2"),
                        video(4, shortNumber = "Court-métrage"),
                        video(5, shortNumber = "Opening 1"),
                        video(6, shortNumber = "Making-of 1")
                    )
                )
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(listOf("1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should exclude every video of a show without the animation genre")
        fun `should exclude every video of a show without the animation genre`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1, showId = 10, showGenres = listOf("Live Action", "Drama")),
                        video(2, showId = 10, showGenres = listOf("Live Action", "Drama")),
                        video(3, showId = 20)
                    )
                )
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(listOf("3"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should exclude every video of a show with an empty genre list")
        fun `should exclude every video of a show with an empty genre list`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, showId = 10, showGenres = emptyList()))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(0, episodes.size)
        }

        @Test
        @DisplayName("should map a single video per language instead of one episode per audio locale")
        fun `should map a single video per language instead of one episode per audio locale`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, shortNumber = "105"), video(2, shortNumber = "106"))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals(listOf("1", "2"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should match trailer keywords on the short number prefix only")
        fun `should match trailer keywords on the short number prefix only`() = runTest {
            // Given the trailer filter is a prefix match, not a containment match
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1, shortNumber = "12"),
                        video(2, shortNumber = "Épisode 2 (Making-of)"),
                        video(3, shortNumber = "Opening 2020"),
                        video(4, shortNumber = "Bande-annonce de l'épisode 4")
                    )
                )
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then only the videos whose short number starts with a trailer keyword are dropped
            assertEquals(listOf("1", "2"), episodes.map { it.id })
        }
    }

    @Nested
    @DisplayName("tests for the show conversion")
    inner class ShowConversionTests {

        @Test
        @DisplayName("should build the anime thumbnail from the portrait with logo source")
        fun `should build the anime thumbnail from the portrait with logo source`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, showId = 77))))

            // When
            val thumbnail = platform.fetchLatestEpisodes().single().anime.thumbnail

            // Then
            assertEquals(
                "https://image.animationdigitalnetwork.com/show/77/1080x1543/portrait-with-logo.width=1080,height=1543,quality=100",
                thumbnail
            )
        }

        @Test
        @DisplayName("should return a null anime description when the show has no summary")
        fun `should return a null anime description when the show has no summary`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, showSummary = null))))

            // When
            val description = platform.fetchLatestEpisodes().single().anime.description

            // Then
            assertEquals(null, description)
        }

        @Test
        @DisplayName("should keep a null episode description when the video has no summary")
        fun `should keep a null episode description when the video has no summary`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, summary = null))))

            // When
            val description = platform.fetchLatestEpisodes().single().description

            // Then
            assertEquals(null, description)
        }
    }

    @Nested
    @DisplayName("tests for the current unported behaviour")
    inner class KnownGapsTests {

        @Test
        @DisplayName("should not filter out not simulcasted shows")
        fun `should not filter out not simulcasted shows`() = runTest {
            // Given a show released long before the current year and not flagged as simulcast
            val platform = platform(smartHttpClient(calendar(video(1, showId = 5, showTitle = "Old Anime", showGenres = listOf("Animation japonaise")))))

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then the platform does not implement the simulcast filter yet, unlike master
            assertEquals(listOf("1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should not expose the episode number season audio locale or uncensored flags")
        fun `should not expose the episode number season audio locale or uncensored flags`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, shortNumber = "Film 1", showId = 1301, showTitle = "OVERLORD: The Sacred Kingdom"))))

            // When
            val episode = platform.fetchLatestEpisodes().single()

            // Then PlatformEpisode only carries the platform identity and its content
            assertEquals("1", episode.id)
            assertEquals("OVERLORD: The Sacred Kingdom", episode.anime.title)
            assertTrue(PlatformEpisode::class.java.declaredFields.none { it.name in setOf("number", "season", "episodeType", "audioLocale", "uncensored", "duration", "url") })
        }
    }
}
