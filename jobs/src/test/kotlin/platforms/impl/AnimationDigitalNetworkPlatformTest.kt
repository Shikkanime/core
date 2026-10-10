package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.EpisodeType
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration

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
        title: String? = null,
        shortNumber: String = "$id",
        season: String? = null,
        type: AdnType = AdnType.EPS,
        image2x: String = "https://image.animationdigitalnetwork.com/video/$id/100x100/eps",
        summary: String? = "Summary $id",
        releaseDate: String = "2026-01-10T08:00:00Z",
        duration: Long = 1430L,
        url: String = "https://animationdigitalnetwork.fr/video/$id",
        languages: List<String> = listOf("vostf"),
        showId: Int = 1,
        showTitle: String = "One Piece",
        showShortTitle: String? = null,
        showSummary: String? = "Pirates summary",
        showImageHorizontal2x: String? = null,
        showType: AdnType? = null,
        showGenres: List<String> = listOf("Animation japonaise", "Action")
    ): String = """
        {
          "id": $id,
          "name": "$name",
          "title": ${title?.let { "\"$it\"" } ?: "null"},
          "shortNumber": "$shortNumber",
          "season": ${season?.let { "\"$it\"" } ?: "null"},
          "type": "${type.name}",
          "image2x": "$image2x",
          "summary": ${summary?.let { "\"$it\"" } ?: "null"},
          "releaseDate": "$releaseDate",
          "duration": $duration,
          "url": "$url",
          "languages": [${languages.joinToString(", ") { "\"$it\"" }}],
          "show": {
            "id": $showId,
            "title": "$showTitle",
            "shortTitle": ${showShortTitle?.let { "\"$it\"" } ?: "null"},
            "originalTitle": null,
            "summary": ${showSummary?.let { "\"$it\"" } ?: "null"},
            "image2x": "https://image.animationdigitalnetwork.com/show/$showId/100x100/portrait-with-logo",
            "imageHorizontal2x": ${showImageHorizontal2x?.let { "\"$it\"" } ?: "null"},
            "type": ${showType?.let { "\"${it.name}\"" } ?: "null"},
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
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1001, name = "Major 105", shortNumber = "105"),
                        video(1002, name = "Autre 3", shortNumber = "3")
                    )
                )
            )

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
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1001, releaseDate = "2026-01-10T08:00:00Z"),
                        video(1002, releaseDate = "2026-06-15T16:30:00Z")
                    )
                )
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals("2026-01-10T08:00:00Z", episodes[0].releaseDateTime
                .toInstant()
                .toString())
            assertEquals("2026-06-15T16:30:00Z", episodes[1].releaseDateTime
                .toInstant()
                .toString())
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
                "https://image.animationdigitalnetwork.com/" +
                    "video/1001/1920x1080/eps.width=1920,height=1080,quality=100",
                episodes.single().image
            )
        }

        @Test
        @DisplayName("should map the show as the platform anime of the episode")
        fun `should map the show as the platform anime of the episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(
                            1001,
                            showId = 1353,
                            showTitle = "Major",
                            showSummary = "A baseball story\nwith two lines"
                        )
                    )
                )
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

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
            assertTrue(
                requestedUrl!!.startsWith("https://gw.api.animationdigitalnetwork.com/video/calendar?date="),
                "unexpected url: $requestedUrl"
            )
        }
    }

    @Nested
    @DisplayName("tests for the video filtering")
    inner class VideoFilteringTests {

        @Test
        @DisplayName("should exclude promotional videos")
        fun `should exclude promotional videos`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1, type = AdnType.EPS),
                        video(2, type = AdnType.PV)
                    )
                )
            )

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
            val platform = platform(
                smartHttpClient(calendar(video(1, showId = 10, showGenres = emptyList())))
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(0, episodes.size)
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
            val thumbnail = platform
                .fetchLatestEpisodes()
                .single()
                .anime.thumbnail

            // Then
            assertEquals(
                "https://image.animationdigitalnetwork.com/" +
                    "show/77/1080x1543/portrait-with-logo.width=1080,height=1543,quality=100",
                thumbnail
            )
        }

        @Test
        @DisplayName("should return a null anime description when the show has no summary")
        fun `should return a null anime description when the show has no summary`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, showSummary = null))))

            // When
            val description = platform
                .fetchLatestEpisodes()
                .single()
                .anime.description

            // Then
            assertEquals(null, description)
        }

        @Test
        @DisplayName("should keep a null episode description when the video has no summary")
        fun `should keep a null episode description when the video has no summary`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1, summary = null))))

            // When
            val description = platform
                .fetchLatestEpisodes()
                .single()
                .description

            // Then
            assertEquals(null, description)
        }
    }

    @Nested
    @DisplayName("tests for episode fields mapping")
    inner class EpisodeFieldsMappingTests {

        @Test
        @DisplayName("should map season number episodeType duration url and audioLocale")
        fun `should map season number episodeType duration url and audioLocale`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(
                            id = 10,
                            name = "The Beginning",
                            shortNumber = "12",
                            season = "2",
                            duration = 1420L,
                            url = "https://animationdigitalnetwork.fr/video/one-piece/10-the-beginning",
                            languages = listOf("vostf")
                        )
                    )
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(2, episode.season)
            assertEquals(12, episode.number)
            assertEquals(EpisodeType.EPISODE, episode.episodeType)
            assertEquals(1420L, episode.duration)
            assertEquals("https://animationdigitalnetwork.fr/video/one-piece/10-the-beginning", episode.url)
            assertEquals("ja-JP", episode.audioLocale)
            assertTrue(episode.original)
            assertFalse(episode.uncensored)
        }

        @Test
        @DisplayName("should map episode type as film when shortNumber contains Film")
        fun `should map episode type as film when shortNumber contains Film`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, shortNumber = "Film 1", showTitle = "OVERLORD: The Sacred Kingdom"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.FILM, episode.episodeType)
            assertEquals(1, episode.number)
        }

        @Test
        @DisplayName("should map episode type as special when shortNumber contains Episode special")
        fun `should map episode type as special when shortNumber contains Episode special`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, shortNumber = "Épisode spécial 13"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
            assertEquals(13, episode.number)
        }

        @Test
        @DisplayName("should map episode type as special when shortNumber contains OAV")
        fun `should map episode type as special when shortNumber contains OAV`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, shortNumber = "OAV 2"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
            assertEquals(2, episode.number)
        }

        @Test
        @DisplayName("should map episode type as special when shortNumber contains a dot")
        fun `should map episode type as special when shortNumber contains a dot`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, shortNumber = "12.5"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
            assertEquals(-1, episode.number)
        }

        @Test
        @DisplayName("should map episode type as film when video type is MOV")
        fun `should map episode type as film when video type is MOV`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, type = AdnType.MOV, shortNumber = "1"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.FILM, episode.episodeType)
            assertEquals(1, episode.number)
        }

        @Test
        @DisplayName("should map episode type as special when video type is OAV")
        fun `should map episode type as special when video type is OAV`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, type = AdnType.OAV, shortNumber = "1"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
            assertEquals(1, episode.number)
        }

        @Test
        @DisplayName("should map episode type as film when show type is MOV")
        fun `should map episode type as film when show type is MOV`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, showType = AdnType.MOV, shortNumber = "1"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.FILM, episode.episodeType)
            assertEquals(1, episode.number)
        }

        @Test
        @DisplayName("should map episode type as special when show type is OAV")
        fun `should map episode type as special when show type is OAV`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, showType = AdnType.OAV, shortNumber = "1"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
            assertEquals(1, episode.number)
        }

        @Test
        @DisplayName("should fallback to minus one when episode number is unparseable")
        fun `should fallback to minus one when episode number is unparseable`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, shortNumber = "Film"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(EpisodeType.FILM, episode.episodeType)
            assertEquals(-1, episode.number)
        }

        @Test
        @DisplayName("should detect uncensored flag when title contains NC tag")
        fun `should detect uncensored flag when title contains NC tag`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, title = "High School DxD - Épisode 1 (NC)"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertTrue(episode.uncensored)
        }

        @Test
        @DisplayName("should detect uncensored flag when title contains Non censuré")
        fun `should detect uncensored flag when title contains Non censuré`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, title = "High School DxD - Épisode 1 Non censuré"))
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertTrue(episode.uncensored)
        }

        @Test
        @DisplayName("should fan out multiple episodes when a video has multiple languages")
        fun `should fan out multiple episodes when a video has multiple languages`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, languages = listOf("vostf", "vf")))
                )
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals("ja-JP", episodes[0].audioLocale)
            assertTrue(episodes[0].original)
            assertEquals("fr-FR", episodes[1].audioLocale)
            assertFalse(episodes[1].original)
        }

        @Test
        @DisplayName("should use direct adn image url when image url does not contain video path")
        fun `should use direct adn image url when image url does not contain video path`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(
                            1,
                            image2x = "https://image.animationdigitalnetwork.fr/" +
                                "license/sample/web/affiche.jpg"
                        )
                    )
                )
            )

            // When
            val episode = platform
                .fetchLatestEpisodes()
                .single()

            // Then
            assertEquals(
                "https://image.animationdigitalnetwork.fr/license/sample/web/affiche.jpg",
                episode.image
            )
        }
    }

    @Nested
    @DisplayName("tests for show fields and name cleaning")
    inner class ShowNameAndBannerTests {

        @Test
        @DisplayName("should clean anime name by removing season suffix")
        fun `should clean anime name by removing season suffix`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, showTitle = "Overlord Saison 4", season = "4"))
                )
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

            // Then
            assertEquals("Overlord", anime.title)
        }

        @Test
        @DisplayName("should clean anime name by removing part suffix")
        fun `should clean anime name by removing part suffix`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, showTitle = "Attack on Titan Part 2"))
                )
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

            // Then
            assertEquals("Attack on Titan", anime.title)
        }

        @Test
        @DisplayName("should clean anime name by removing roman numerals suffix")
        fun `should clean anime name by removing roman numerals suffix`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, showTitle = "Sword Art Online II"))
                )
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

            // Then
            assertEquals("Sword Art Online", anime.title)
        }

        @Test
        @DisplayName("should prefer show shortTitle over title when present")
        fun `should prefer show shortTitle over title when present`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(
                            1,
                            showTitle = "One Piece : Saga 15 - Egghead",
                            showShortTitle = "One Piece"
                        )
                    )
                )
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

            // Then
            assertEquals("One Piece", anime.title)
        }

        @Test
        @DisplayName("should map anime banner from show imageHorizontal2x")
        fun `should map anime banner from show imageHorizontal2x`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(
                            1,
                            showImageHorizontal2x = "https://image.animationdigitalnetwork.com/" +
                                "show/1/640x360/landscape-with-logo"
                        )
                    )
                )
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

            // Then
            assertEquals(
                "https://image.animationdigitalnetwork.com/" +
                    "show/1/1920x1080/landscape-with-logo.width=1920,height=1080,quality=100",
                anime.banner
            )
        }

        @Test
        @DisplayName("should keep anime banner null when imageHorizontal2x is absent")
        fun `should keep anime banner null when imageHorizontal2x is absent`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(calendar(video(1, showImageHorizontal2x = null)))
            )

            // When
            val anime = platform
                .fetchLatestEpisodes()
                .single()
                .anime

            // Then
            assertNull(anime.banner)
        }
    }

    @Nested
    @DisplayName("tests for the current unported behaviour")
    inner class KnownGapsTests {

        @Test
        @DisplayName("should not filter out not simulcasted shows")
        fun `should not filter out not simulcasted shows`() = runTest {
            // Given a show released long before the current year and not flagged as simulcast
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(
                            1,
                            showId = 5,
                            showTitle = "Old Anime",
                            showGenres = listOf("Animation japonaise")
                        )
                    )
                )
            )

            // When
            val episodes = platform.fetchLatestEpisodes()

            // Then the platform does not implement the simulcast filter yet, unlike master
            assertEquals(listOf("1"), episodes.map { it.id })
        }
    }
}
