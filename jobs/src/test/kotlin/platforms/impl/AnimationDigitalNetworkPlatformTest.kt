package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.builders.impl.AdnShowMockKBuilder
import fr.shikkanime.jobs.builders.impl.AdnVideoMockKBuilder
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.PlatformEpisodeLocalization
import fr.shikkanime.jobs.platforms.exceptions.MissingAudioLocaleException
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration

@ExtendWith(MockKExtension::class)
@DisplayName("tests for AnimationDigitalNetworkPlatform")
class AnimationDigitalNetworkPlatformTest {

    /**
     * Builds a [SmartHttpClient] backed by a mock engine answering with the provided JSON body.
     *
     * @param onRequest Callback observing every outgoing request before the answer is produced.
     * @param bodyProvider Produces the JSON response body for a given request.
     * @return The [SmartHttpClient] under test.
     * @see [SmartHttpClient]
     */
    private fun smartHttpClient(
        onRequest: (HttpRequestData) -> Unit = {},
        bodyProvider: (HttpRequestData) -> String
    ): SmartHttpClient =
        SmartHttpClient(
            client = HttpClient(
                MockEngine { request ->
                    onRequest(request)
                    respond(
                        content = bodyProvider(request),
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

    /**
     * Builds a [SmartHttpClient] backed by a mock engine answering with a constant JSON body.
     *
     * @param body The JSON response body returned for every request.
     * @return The [SmartHttpClient] under test.
     * @see [SmartHttpClient]
     */
    private fun smartHttpClient(body: String): SmartHttpClient =
        smartHttpClient { body }

    /**
     * Builds the [AnimationDigitalNetworkPlatform] under test.
     *
     * @param client The [SmartHttpClient] serving the calendar payloads.
     * @param countries The configured target countries of the platform.
     * @return The [AnimationDigitalNetworkPlatform] under test.
     * @see [AnimationDigitalNetworkPlatform]
     */
    private fun platform(
        client: SmartHttpClient,
        countries: Set<Country> = setOf(Country.FR)
    ): AnimationDigitalNetworkPlatform =
        AnimationDigitalNetworkPlatform(
            client = client,
            ttl = Duration.ZERO,
            countries = countries
        )

    /**
     * Builds an [AdnShow] fixture through the [AdnShowMockKBuilder].
     *
     * @param id The identifier of the show.
     * @param title The title of the show.
     * @param shortTitle The short title of the show.
     * @param summary The optional summary of the show.
     * @param type The content type of the show.
     * @param genres The genres of the show.
     * @return The mocked [AdnShow] fixture.
     * @see [AdnShowMockKBuilder]
     * @see [AdnShow]
     */
    private fun adnShow(
        id: Int = 1,
        title: String = "One Piece",
        shortTitle: String? = "One Piece",
        summary: String? = "Pirates summary",
        type: AdnType = AdnType.EPS,
        genres: List<String> = listOf("Animation ")
    ): AdnShow =
        AdnShowMockKBuilder {
            this.id = id
            this.title = title
            this.shortTitle = shortTitle
            this.summary = summary
            this.type = type
            this.genres = genres
            thumbnail = "https://image.animationdigitalnetwork.com/show/$id/1080x1543/portrait"
            banner = "https://image.animationdigitalnetwork.com/show/$id/1920x1080/landscape"
        }.build()

    /**
     * Builds an [AdnVideo] fixture through the [AdnVideoMockKBuilder] with its parent show.
     *
     * @param id The identifier of the video.
     * @param name The name of the video.
     * @param title The optional title of the video.
     * @param languages The audio language codes of the video.
     * @param show The parent [AdnShow] fixture of the video.
     * @return The mocked [AdnVideo] fixture.
     * @see [AdnVideoMockKBuilder]
     * @see [AdnShow]
     */
    private fun adnVideo(
        id: Int = 1001,
        name: String = "Major 105",
        title: String? = null,
        languages: List<String> = listOf("vostde"),
        show: AdnShow = adnShow()
    ): AdnVideo =
        AdnVideoMockKBuilder {
            this.id = id
            this.name = name
            this.title = title
            shortNumber = "105"
            season = "1"
            type = AdnType.EPS
            image = "https://image.animationdigitalnetwork.com/video/$id/1920x1080/" +
                "eps.width=1920,height=1080,quality=100"
            releaseDate = ZonedDateTime.now()
            duration = 1420L
            url = "https://example.com/video/$id"
            this.languages = languages
            this.show = show
        }.build()

    /**
     * Serializes an ADN calendar video payload with the given field values.
     *
     * @param id The identifier of the video.
     * @param name The name of the video.
     * @param title The optional title of the video.
     * @param shortNumber The short number of the video.
     * @param season The optional season of the video.
     * @param type The content type of the video.
     * @param image2x The source image URL of the video.
     * @param summary The optional summary of the video.
     * @param releaseDate The ISO release date of the video.
     * @param duration The duration of the video in seconds.
     * @param url The URL of the video.
     * @param languages The audio language codes of the video.
     * @param showId The identifier of the parent show.
     * @param showTitle The title of the parent show.
     * @param showShortTitle The optional short title of the parent show.
     * @param showSummary The optional summary of the parent show.
     * @param showImageHorizontal2x The source landscape image URL of the parent show.
     * @param showType The content type of the parent show.
     * @param showGenres The genres of the parent show.
     * @return The JSON payload of the calendar video.
     * @see [AdnVideo]
     */
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
        showImageHorizontal2x: String = "https://image.animationdigitalnetwork.com/" +
            "show/$showId/100x100/landscape-with-logo",
        showType: AdnType = AdnType.EPS,
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
            "imageHorizontal2x": "$showImageHorizontal2x",
            "type": "${showType.name}",
            "genres": [${showGenres.joinToString(", ") { "\"$it\"" }}]
          }
        }
    """.trimIndent()

    /**
     * Wraps the given video payloads into an ADN calendar JSON body.
     *
     * @param videos The serialized video payloads of the calendar.
     * @return The JSON body of the calendar response.
     * @see [video]
     */
    private fun calendar(vararg videos: String): String =
        """{"videos": [${videos.joinToString(",")}]}"""

    /**
     * Harvests the episodes of the platform, leaving the change flag aside.
     *
     * The mapping assertions only read the harvested episodes; the change flag of the payload is
     * asserted through [AnimationDigitalNetworkPlatform.fetchLatestEpisodes] itself.
     *
     * @receiver the streaming platform to harvest.
     * @return the harvested episodes.
     * @see [AnimationDigitalNetworkPlatform.fetchLatestEpisodes]
     */
    private suspend fun AnimationDigitalNetworkPlatform.episodes(): List<PlatformEpisode> =
        fetchLatestEpisodes().data

    @Nested
    @DisplayName("tests for the 'fetchLatestEpisodes' method")
    inner class FetchLatestEpisodesTests {

        @Test
        @DisplayName("should report a change on the first harvest")
        fun `should report a change on the first harvest`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001))))

            // When
            val harvest = platform.fetchLatestEpisodes()

            // Then: the payload is fetched for the first time, so its content changed
            assertTrue(harvest.hasChanged)
            assertEquals(1, harvest.data.size)
        }

        @Test
        @DisplayName("should report no change while the calendar content is unchanged")
        fun `should report no change while the calendar content is unchanged`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001))))
            platform.fetchLatestEpisodes()

            // When
            val harvest = platform.fetchLatestEpisodes()

            // Then: the cached payload reports no change
            assertFalse(harvest.hasChanged)
        }

        @Test
        @DisplayName("should report a change when the calendar content changes")
        fun `should report a change when the calendar content changes`() = runTest {
            // Given
            var body = calendar(video(1001))
            val platform = platform(smartHttpClient { body })
            platform.fetchLatestEpisodes()

            // When
            body = calendar(video(2002))
            val harvest = platform.fetchLatestEpisodes()

            // Then
            assertTrue(harvest.hasChanged)
            assertEquals("2002", harvest.data.single().id)
        }

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
            val episodes = platform.episodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals("1001", episodes[0].id)
            assertEquals("Major 105", episodes[0].localizations.single().title)
            assertEquals("1002", episodes[1].id)
            assertEquals("Autre 3", episodes[1].localizations.single().title)
        }

        @Test
        @DisplayName("should return an empty list when the calendar has no videos")
        fun `should return an empty list when the calendar has no videos`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar()))

            // When
            val episodes = platform.episodes()

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
        @DisplayName("should expose france as the default configured country for adn")
        fun `should expose france as the default configured country for adn`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar()))

            // When
            val actual = platform.countries

            // Then
            assertEquals(setOf(Country.FR), actual)
        }

        @Test
        @DisplayName("should map configured country on episode localizations")
        fun `should map configured country on episode localizations`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001))))

            // When
            val episode = platform.episodes()
                .single()

            // Then
            val localization = episode.localizations.single()
            assertEquals(Country.FR, localization.country)
            assertEquals("Episode 1001", localization.title)
        }

        @Test
        @DisplayName("should fetch episodes for configured countries")
        fun `should fetch episodes for configured countries`() = runTest {
            // Given
            val customCountries = setOf(Country.FR, Country.BE)
            val platform = platform(
                smartHttpClient(calendar(video(1001))),
                countries = customCountries
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(1, episodes.size)
            val episode = episodes.single()
            assertEquals(2, episode.localizations.size)
            assertTrue(episode.localizations.any { it.country == Country.FR })
            assertTrue(episode.localizations.any { it.country == Country.BE })
        }

        @Test
        @DisplayName("should merge audio locales across multiple countries for the same episode")
        fun `should merge audio locales across multiple countries for the same episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient { request ->
                    val dist = request.headers["X-Target-Distribution"]
                    if (dist == "de")
                        calendar(video(1001, languages = listOf("vostde", "vde")))
                    else
                        calendar(video(1001, languages = listOf("vostf", "vf")))
                },
                countries = setOf(Country.FR, Country.DE)
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(1, episodes.size)
            val episode = episodes.single()
            assertEquals(
                listOf(Locale.JA_JP, Locale.FR_FR, Locale.DE_DE),
                episode.audioLocales
            )
            assertEquals(2, episode.localizations.size)
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
            val episodes = platform.episodes()

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
            val episodes = platform.episodes()

            // Then
            assertEquals("A great episode", episodes.single().localizations.single().description)
            assertEquals(
                "https://image.animationdigitalnetwork.com/" +
                    "video/1001/1920x1080/eps.width=1920,height=1080,quality=100",
                episodes.single().image
            )
        }

        @Test
        @DisplayName("should fall back to the video title when the name is blank")
        fun `should fall back to the video title when the name is blank`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, name = "", title = "TOUGEN ANKI - Épisode 5"))
                )
            )

            // When
            val title = platform
                .episodes()
                .single()
                .localizations
                .single()
                .title

            // Then
            assertEquals("TOUGEN ANKI - Épisode 5", title)
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
                .episodes()
                .single()
                .anime

            // Then
            assertEquals("1353", anime.id)
            assertEquals("Major", anime.localizations.single().title)
            assertEquals(
                "A baseball storywith two lines",
                anime.localizations.single().description
            )
        }

        @Test
        @DisplayName("should reuse the same mapped list instance while the calendar content is unchanged")
        fun `should reuse the same mapped list instance while the calendar content is unchanged`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar(video(1001))))

            // When
            val first = platform.episodes()
            val second = platform.episodes()

            // Then
            assertSame(first, second)
        }

        @Test
        @DisplayName("should reuse the same empty list instance when calendar has no releases")
        fun `should reuse the same empty list instance when calendar has no releases`() = runTest {
            // Given
            val platform = platform(smartHttpClient(calendar()))

            // When
            val first = platform.episodes()
            val second = platform.episodes()

            // Then
            assertSame(first, second)
            assertTrue(first.isEmpty())
        }

        @Test
        @DisplayName("should map again when the calendar content changes")
        fun `should map again when the calendar content changes`() = runTest {
            // Given
            var body = calendar(video(1001))
            val platform = platform(smartHttpClient { body })
            val first = platform.episodes()

            // When
            body = calendar(video(2002))
            val second = platform.episodes()

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
            platform.episodes()

            // Then
            assertEquals("fr", headers[HttpHeaders.AcceptLanguage])
            assertEquals("Web", headers["X-Source"])
            assertEquals("fr", headers["X-Target-Distribution"])
            assertTrue(
                requestedUrl!!.startsWith("https://gw.api.animationdigitalnetwork.com/video/calendar?date="),
                "unexpected url: $requestedUrl"
            )
        }

        @Test
        @DisplayName("should send target distribution headers matching the configured country")
        fun `should send target distribution headers matching the configured country`() = runTest {
            // Given
            val headers = mutableMapOf<String, String>()
            val platform = platform(
                smartHttpClient(
                    bodyProvider = { calendar(video(1001, languages = listOf("vostde"))) },
                    onRequest = { request ->
                        request.headers.forEach { key, values ->
                            headers[key] = values.joinToString(",")
                        }
                    }
                ),
                countries = setOf(Country.DE)
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals("de", headers[HttpHeaders.AcceptLanguage])
            assertEquals("Web", headers["X-Source"])
            assertEquals("de", headers["X-Target-Distribution"])
            val localization = episodes.single().localizations.single()
            assertEquals(Country.DE, localization.country)
            assertEquals(listOf(Locale.JA_JP), episodes.single().audioLocales)
        }

        @Test
        @DisplayName("should map german audio tracks appropriately")
        fun `should map german audio tracks appropriately`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(calendar(video(1001, languages = listOf("vostde", "vde")))),
                countries = setOf(Country.DE)
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(1, episodes.size)
            val episode = episodes.single()
            val localization = episode.localizations.single()
            assertEquals(Country.DE, localization.country)
            assertEquals(listOf(Locale.JA_JP, Locale.DE_DE), episode.audioLocales)
        }

        @Test
        @DisplayName("should skip episodes with missing audio locale without breaking calendar")
        fun `should skip episodes with missing audio locale without breaking calendar`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(
                        video(1001, name = "Invalid", languages = listOf("vostde")),
                        video(1002, name = "Valid", languages = listOf("vostf"))
                    )
                ),
                countries = setOf(Country.FR)
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(1, episodes.size)
            assertEquals("1002", episodes.single().id)
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
            val episodes = platform.episodes()

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
            val episodes = platform.episodes()

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
            val episodes = platform.episodes()

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
            val episodes = platform.episodes()

            // Then
            assertEquals(0, episodes.size)
        }

        @Test
        @DisplayName("should keep a show whose animation genre has no suffix")
        fun `should keep a show whose animation genre has no suffix`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(calendar(video(1, showGenres = listOf("Animation"))))
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("1"), episodes.map { it.id })
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
            val episodes = platform.episodes()

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
                .episodes()
                .single()
                .anime.images.single { it.type == ImageType.THUMBNAIL }.url

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
                .episodes()
                .single()
                .anime.localizations.single().description

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
                .episodes()
                .single()
                .localizations
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
        @DisplayName("should map season number episodeType duration url and audioLocales")
        fun `should map season number episodeType duration url and audioLocales`() = runTest {
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
                .episodes()
                .single()

            // Then
            assertEquals(2, episode.season)
            assertEquals(12, episode.number)
            assertEquals(EpisodeType.EPISODE, episode.episodeType)
            assertEquals(1420L, episode.duration)
            assertEquals("https://animationdigitalnetwork.fr/video/one-piece/10-the-beginning", episode.url)
            assertEquals(listOf(Locale.JA_JP), episode.audioLocales)
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
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
                .episodes()
                .single()

            // Then
            assertTrue(episode.uncensored)
        }

        @Test
        @DisplayName("should produce single episode when a video has multiple languages")
        fun `should produce single episode when a video has multiple languages`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    calendar(video(1, languages = listOf("vostf", "vf")))
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(1, episodes.size)
            assertEquals(listOf(Locale.JA_JP, Locale.FR_FR), episodes.single().audioLocales)
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
                .episodes()
                .single()

            // Then
            assertEquals(
                "https://image.animationdigitalnetwork.fr/license/sample/web/affiche.jpg",
                episode.image
            )
        }

        @Test
        @DisplayName("should throw MissingAudioLocaleException when video languages is empty")
        fun `should throw MissingAudioLocaleException when video languages is empty`() {
            // Given
            val video = adnVideo(1001, name = "Major 105", title = "Major 105", languages = emptyList())
            val anime = video.show.toPlatformAnime(1, Country.FR)

            // When & Then
            val exception = assertFailsWith<MissingAudioLocaleException> {
                video.toPlatformEpisode(anime, Country.FR)
            }
            assertEquals("1001", exception.episodeId)
            assertEquals("Major 105", exception.episodeName)
            assertTrue(exception.languages.isEmpty())
            assertEquals(Country.FR, exception.country)
            assertTrue(exception.message!!.contains("1001"))
            assertTrue(exception.message!!.contains("Major 105"))
            assertTrue(exception.message!!.contains("FR"))
        }

        @Test
        @DisplayName("should throw MissingAudioLocaleException when languages are incompatible")
        fun `should throw MissingAudioLocaleException when languages are incompatible`() {
            // Given
            val video = adnVideo(1002, name = "Episode 2", title = "Episode 2", languages = listOf("vostde"))
            val anime = video.show.toPlatformAnime(1, Country.FR)

            // When & Then
            val exception = assertFailsWith<MissingAudioLocaleException> {
                video.toPlatformEpisode(anime, Country.FR)
            }
            assertEquals("1002", exception.episodeId)
            assertEquals("Episode 2", exception.episodeName)
            assertEquals(listOf("vostde"), exception.languages)
            assertEquals(Country.FR, exception.country)
        }

        @Test
        @DisplayName("should throw MissingAudioLocaleException with the video title when name is blank")
        fun `should throw MissingAudioLocaleException with the video title when name is blank`() {
            // Given
            val video = adnVideo(1003, name = "", title = "The Beginning", languages = emptyList())
            val anime = video.show.toPlatformAnime(1, Country.FR)

            // When & Then
            val exception = assertFailsWith<MissingAudioLocaleException> {
                video.toPlatformEpisode(anime, Country.FR)
            }
            assertEquals("1003", exception.episodeId)
            assertEquals("The Beginning", exception.episodeName)
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
                .episodes()
                .single()
                .anime

            // Then
            assertEquals("Overlord", anime.localizations.single().title)
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
                .episodes()
                .single()
                .anime

            // Then
            assertEquals("Attack on Titan", anime.localizations.single().title)
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
                .episodes()
                .single()
                .anime

            // Then
            assertEquals("Sword Art Online", anime.localizations.single().title)
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
                .episodes()
                .single()
                .anime

            // Then
            assertEquals("One Piece", anime.localizations.single().title)
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
                .episodes()
                .single()
                .anime

            // Then
            assertEquals(
                "https://image.animationdigitalnetwork.com/" +
                    "show/1/1920x1080/landscape-with-logo.width=1920,height=1080,quality=100",
                anime.images.single { it.type == ImageType.BANNER }.url
            )
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
            val episodes = platform.episodes()

            // Then the platform does not implement the simulcast filter yet, unlike master
            assertEquals(listOf("1"), episodes.map { it.id })
        }
    }
}
