package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.platforms.PlatformEpisode
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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration

@ExtendWith(MockKExtension::class)
@DisplayName("tests for CrunchyrollPlatform")
class CrunchyrollPlatformTest {

    /**
     * Builds a [SmartHttpClient] backed by a mock engine answering with the provided JSON body.
     *
     * @param onRequest Callback observing every outgoing request before the answer is produced.
     * @param bodyProvider Produces the JSON response body for a given request.
     * @return The [SmartHttpClient] under test.
     * @see [SmartHttpClient]
     */
    private fun rawSmartHttpClient(
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
     * Builds a [SmartHttpClient] backed by a mock engine routing the Crunchyroll endpoints.
     *
     * The token endpoint answers with a fixed token, the discover browse endpoint with the given
     * episode browse objects and the CMS objects endpoint with the series browse objects matching
     * the requested identifier.
     *
     * @param browseObjects The serialized episode browse objects of the discover response.
     * @param seriesObjects The serialized series browse objects of the CMS objects responses.
     * @param onRequest Callback observing every outgoing request before the answer is produced.
     * @return The [SmartHttpClient] under test.
     * @see [SmartHttpClient]
     * @see [episode]
     * @see [series]
     */
    private fun smartHttpClient(
        browseObjects: List<String> = emptyList(),
        seriesObjects: List<String> = emptyList(),
        onRequest: (HttpRequestData) -> Unit = {}
    ): SmartHttpClient =
        rawSmartHttpClient(onRequest) { request ->
            val url = request.url.toString()
            when {
                request.method == HttpMethod.Post -> TOKEN_RESPONSE
                "/discover/browse" in url -> browseResponse(*browseObjects.toTypedArray())
                "/cms/objects/" in url -> {
                    val seriesIds = url.substringAfter("/cms/objects/")
                        .substringBefore("?")
                        .split(",")
                    val matched = seriesObjects.filter { fixture ->
                        seriesIds.any { id -> "\"id\": \"$id\"" in fixture }
                    }
                    objectsResponse(*matched.toTypedArray())
                }
                else -> EMPTY_RESPONSE
            }
        }

    /**
     * Builds the [CrunchyrollPlatform] under test.
     *
     * @param client The [SmartHttpClient] serving the Crunchyroll payloads.
     * @param countries The configured target countries of the platform.
     * @return The [CrunchyrollPlatform] under test.
     * @see [CrunchyrollPlatform]
     */
    private fun platform(
        client: SmartHttpClient,
        countries: Set<Country> = setOf(Country.FR)
    ): CrunchyrollPlatform =
        CrunchyrollPlatform(
            client = client,
            ttl = Duration.ZERO,
            countries = countries
        )

    /**
     * Harvests the episodes of the platform, leaving the change flag aside.
     *
     * The mapping assertions only read the harvested episodes; the change flag of the payload is
     * asserted through [CrunchyrollPlatform.fetchLatestEpisodes] itself.
     *
     * @receiver the streaming platform to harvest.
     * @return the harvested episodes.
     * @see [CrunchyrollPlatform.fetchLatestEpisodes]
     */
    private suspend fun CrunchyrollPlatform.episodes(): List<PlatformEpisode> =
        fetchLatestEpisodes().data

    @Nested
    @DisplayName("tests for the 'fetchLatestEpisodes' method")
    inner class FetchLatestEpisodesTests {

        @Test
        @DisplayName("should report a change on the first harvest")
        fun `should report a change on the first harvest`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val harvest = platform.fetchLatestEpisodes()

            // Then: the payload is fetched for the first time, so its content changed
            assertTrue(harvest.hasChanged)
            assertEquals(1, harvest.data.size)
        }

        @Test
        @DisplayName("should report no change while the browse content is unchanged")
        fun `should report no change while the browse content is unchanged`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series())
                )
            )
            platform.fetchLatestEpisodes()

            // When
            val harvest = platform.fetchLatestEpisodes()

            // Then: the cached payload reports no change
            assertFalse(harvest.hasChanged)
        }

        @Test
        @DisplayName("should report a change when the browse content changes")
        fun `should report a change when the browse content changes`() = runTest {
            // Given
            var browseObjects = listOf(episode(id = "EP1"))
            val platform = platform(
                rawSmartHttpClient { request ->
                    val url = request.url.toString()
                    when {
                        request.method == HttpMethod.Post -> TOKEN_RESPONSE
                        "/discover/browse" in url -> browseResponse(*browseObjects.toTypedArray())
                        "/cms/objects/" in url -> objectsResponse(series())
                        else -> EMPTY_RESPONSE
                    }
                }
            )
            platform.fetchLatestEpisodes()

            // When
            browseObjects = listOf(episode(id = "EP2"))
            val harvest = platform.fetchLatestEpisodes()

            // Then
            assertTrue(harvest.hasChanged)
            assertEquals("EP2", harvest.data.single().id)
        }

        @Test
        @DisplayName("should map every episode of the browse page to a platform episode")
        fun `should map every episode of the browse page to a platform episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", numberString = "1", number = 1),
                        episode(id = "EP2", seriesId = "SERIES2", numberString = "2", number = 2)
                    ),
                    seriesObjects = listOf(series(), series(id = "SERIES2", title = "Naruto"))
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(2, episodes.size)
            assertEquals("EP1", episodes[0].id)
            assertEquals("EP2", episodes[1].id)
        }

        @Test
        @DisplayName("should resolve every series object in a single batched request")
        fun `should resolve every series object in a single batched request`() = runTest {
            // Given
            var objectsRequests = 0
            var requestedIds: String? = null
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1"),
                        episode(id = "EP2", seriesId = "SERIES2", numberString = "2", number = 2)
                    ),
                    seriesObjects = listOf(series(), series(id = "SERIES2", title = "Naruto")),
                    onRequest = { request ->
                        val url = request.url.toString()
                        if ("/cms/objects/" in url) {
                            objectsRequests++
                            requestedIds = url.substringAfter("/cms/objects/").substringBefore("?")
                        }
                    }
                )
            )

            // When
            val episodes = platform.episodes()

            // Then: one request carries every series identifier of the page
            assertEquals(2, episodes.size)
            assertEquals(1, objectsRequests)
            assertEquals("SERIES1,SERIES2", requestedIds)
        }

        @Test
        @DisplayName("should ignore browse objects without episode metadata")
        fun `should ignore browse objects without episode metadata`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(series(id = "SERIES1"), episode()),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should return an empty list when the browse page has no episodes")
        fun `should return an empty list when the browse page has no episodes`() = runTest {
            // Given
            val platform = platform(smartHttpClient())

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(0, episodes.size)
        }

        @Test
        @DisplayName("should reuse the same mapped list instance while the payloads are unchanged")
        fun `should reuse the same mapped list instance while the payloads are unchanged`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val first = platform.episodes()
            val second = platform.episodes()

            // Then
            assertSame(first, second)
        }

        @Test
        @DisplayName("should expose the crunchyroll platform")
        fun `should expose the crunchyroll platform`() = runTest {
            // Given
            val platform = platform(smartHttpClient())

            // When
            val actual = platform.platform

            // Then
            assertEquals(Platform.CRUNCHYROLL, actual)
        }

        @Test
        @DisplayName("should expose france as the default configured country for crunchyroll")
        fun `should expose france as the default configured country for crunchyroll`() = runTest {
            // Given
            val platform = platform(smartHttpClient())

            // When
            val actual = platform.countries

            // Then
            assertEquals(setOf(Country.FR), actual)
        }

        @Test
        @DisplayName("should request the browse catalog with the country locale and a bearer token")
        fun `should request the browse catalog with the country locale and a bearer token`() = runTest {
            // Given
            var requestedUrl: String? = null
            val authorization = mutableListOf<String>()
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series()),
                    onRequest = { request ->
                        if ("/discover/browse" in request.url.toString()) {
                            requestedUrl = request.url.toString()
                            authorization += request.headers[HttpHeaders.Authorization].orEmpty()
                        }
                    }
                )
            )

            // When
            platform.episodes()

            // Then
            assertTrue(requestedUrl!!.contains("locale=fr-FR"), "unexpected url: $requestedUrl")
            assertTrue(requestedUrl!!.contains("type=episode"), "unexpected url: $requestedUrl")
            assertEquals(listOf("Bearer test-access-token"), authorization)
        }

        @Test
        @DisplayName("should mint the access token only once across harvests")
        fun `should mint the access token only once across harvests`() = runTest {
            // Given
            var tokenRequests = 0
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series()),
                    onRequest = { request ->
                        if (request.method == HttpMethod.Post)
                            tokenRequests++
                    }
                )
            )

            // When
            platform.fetchLatestEpisodes()
            platform.fetchLatestEpisodes()
            platform.fetchLatestEpisodes()

            // Then
            assertEquals(1, tokenRequests)
        }

        @Test
        @DisplayName("should send the token request as a form urlencoded POST")
        fun `should send the token request as a form urlencoded POST`() = runTest {
            // Given
            var tokenBody: String? = null
            var tokenContentType: String? = null
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series()),
                    onRequest = { request ->
                        if (request.method == HttpMethod.Post) {
                            (request.body as? TextContent)?.let { content ->
                                tokenBody = content.text
                                tokenContentType = content.contentType.toString()
                            }
                        }
                    }
                )
            )

            // When
            platform.fetchLatestEpisodes()

            // Then
            assertTrue(
                tokenContentType.orEmpty().startsWith("application/x-www-form-urlencoded"),
                "unexpected content type: $tokenContentType"
            )
            assertTrue(
                tokenBody.orEmpty().contains("grant_type=client_id"),
                "unexpected body: $tokenBody"
            )
            assertTrue(tokenBody.orEmpty().contains("client_id=offline_access"))
        }

        @Test
        @DisplayName("should harvest the episodes of every configured country")
        fun `should harvest the episodes of every configured country`() = runTest {
            // Given a Crunchyroll identifier is unique to an audio locale, so each country
            // harvests its own version of the episode
            val platform = platform(
                client = rawSmartHttpClient { request ->
                    val url = request.url.toString()
                    when {
                        request.method == HttpMethod.Post -> TOKEN_RESPONSE
                        "/discover/browse" in url -> {
                            if ("locale=de-DE" in url)
                                browseResponse(
                                    episode(id = "EP1-DE", audioLocale = "de-DE", subtitleLocales = listOf("de-DE"))
                                )
                            else
                                browseResponse(episode(id = "EP1"))
                        }
                        "/cms/objects/" in url -> objectsResponse(series())
                        else -> EMPTY_RESPONSE
                    }
                },
                countries = setOf(Country.FR, Country.DE)
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(2, episodes.size)
            val french = episodes.single { it.id == "EP1" }
            val german = episodes.single { it.id == "EP1-DE" }
            assertEquals(listOf(Locale.FR_FR), french.audioLocales)
            assertEquals(listOf(Locale.DE_DE), german.audioLocales)
            assertEquals(Country.FR, french.localizations.single().country)
            assertEquals(Country.DE, german.localizations.single().country)
        }
    }

    @Nested
    @DisplayName("tests for the episode filtering")
    inner class FilteringTests {

        @Test
        @DisplayName("should skip teaser episodes")
        fun `should skip teaser episodes`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1"),
                        episode(id = "EP2", slugTitle = "one-piece-teaser"),
                        episode(id = "EP3", slugTitle = "one-piece-pv-1"),
                        episode(id = "EP4", slugTitle = "one-piece-trailer")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should skip episodes released at the epoch date")
        fun `should skip episodes released at the epoch date`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1"),
                        episode(id = "EP2", premiumAvailableDate = "1970-01-01T00:00:00Z")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should skip episodes without french subtitles when not dubbed")
        fun `should skip episodes without french subtitles when not dubbed`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1"),
                        episode(
                            id = "EP2",
                            audioLocale = "ja-JP",
                            subtitleLocales = listOf("en-US", "es-419")
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should keep dubbed episodes even without subtitles")
        fun `should keep dubbed episodes even without subtitles`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", subtitleLocales = emptyList())
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should keep episodes subtitled in the country language")
        fun `should keep episodes subtitled in the country language`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "EP1",
                            audioLocale = "ja-JP",
                            subtitleLocales = listOf("fr-FR")
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

        @Test
        @DisplayName("should skip episodes whose series cannot be resolved")
        fun `should skip episodes whose series cannot be resolved`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", seriesId = "UNKNOWN")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(0, episodes.size)
        }

        @Test
        @DisplayName("should skip episodes whose audio locale cannot be mapped")
        fun `should skip episodes whose audio locale cannot be mapped`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1"),
                        episode(id = "EP2", audioLocale = "es-419", subtitleLocales = listOf("fr-FR"))
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf("EP1"), episodes.map { it.id })
        }

    }

    @Nested
    @DisplayName("tests for the episode type detection")
    inner class EpisodeTypeTests {

        @Test
        @DisplayName("should keep films")
        fun `should keep films`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "FILM1",
                            seasonSlugTitle = "one-piece-movie",
                            numberString = "",
                            number = null
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            val episode = episodes.single()
            assertEquals(EpisodeType.FILM, episode.episodeType)
        }

        @Test
        @DisplayName("should detect an unnumbered long episode as a film")
        fun `should detect an unnumbered long episode as a film`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "FILM1",
                            numberString = "",
                            number = null,
                            durationMs = 7_200_000
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(EpisodeType.FILM, episodes.single().episodeType)
        }

        @Test
        @DisplayName("should detect a special episode from its SP number")
        fun `should detect a special episode from its SP number`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "SP1", numberString = "SP2", number = null)
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            val episode = episodes.single()
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
            assertEquals(2, episode.number)
        }

        @Test
        @DisplayName("should detect a special episode from its special season slug")
        fun `should detect a special episode from its special season slug`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "SP1", seasonSlugTitle = "one-piece-specials")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(EpisodeType.SPECIAL, episodes.single().episodeType)
        }

        @Test
        @DisplayName("should detect a special episode from its OAD identifier")
        fun `should detect a special episode from its OAD identifier`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "SP1", identifier = "SERIES|OAD|01")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(EpisodeType.SPECIAL, episodes.single().episodeType)
        }

        @Test
        @DisplayName("should classify a non-numeric episode label as a special")
        fun `should classify a non-numeric episode label as a special`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "SP1", numberString = "Recap", number = 5)
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episode = platform.episodes().single()

            // Then
            assertEquals(EpisodeType.SPECIAL, episode.episodeType)
        }

        @Test
        @DisplayName("should detect a standard episode")
        fun `should detect a standard episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            val episode = episodes.single()
            assertEquals(EpisodeType.EPISODE, episode.episodeType)
            assertEquals(1, episode.number)
        }
    }

    @Nested
    @DisplayName("tests for the audio locales")
    inner class AudioLocaleTests {

        @Test
        @DisplayName("should keep the japanese audio of an episode")
        fun `should keep the japanese audio of an episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", audioLocale = "ja-JP", subtitleLocales = listOf("fr-FR"))
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf(Locale.JA_JP), episodes.single().audioLocales)
        }

        @Test
        @DisplayName("should keep the english audio of an episode")
        fun `should keep the english audio of an episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", audioLocale = "en-US", subtitleLocales = listOf("fr-FR"))
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf(Locale.EN_US), episodes.single().audioLocales)
        }

        @Test
        @DisplayName("should expose a single audio locale per episode identifier")
        fun `should expose a single audio locale per episode identifier`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertEquals(listOf(Locale.FR_FR), episodes.single().audioLocales)
        }
    }

    @Nested
    @DisplayName("tests for the episode fields mapping")
    inner class EpisodeFieldsMappingTests {

        @Test
        @DisplayName("should map the episode fields to the platform episode")
        fun `should map the episode fields to the platform episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "EP1",
                            seasonNumber = 2,
                            numberString = "5",
                            number = 5,
                            durationMs = 1_420_000,
                            premiumAvailableDate = "2026-01-10T08:00:00Z",
                            matureBlocked = true
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episode = platform.episodes().single()

            // Then
            assertEquals("EP1", episode.id)
            assertEquals(2, episode.season)
            assertEquals(5, episode.number)
            assertEquals(EpisodeType.EPISODE, episode.episodeType)
            assertEquals(1420L, episode.duration)
            assertEquals("2026-01-10T08:00:00Z", episode.releaseDateTime.toInstant().toString())
            assertEquals(
                "https://www.crunchyroll.com/fr/watch/EP1/one-piece-episode-1",
                episode.url
            )
            assertTrue(episode.uncensored)
            assertEquals(listOf(Locale.FR_FR), episode.audioLocales)
        }

        @Test
        @DisplayName("should map the episode title description and image")
        fun `should map the episode title description and image`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "EP1",
                            title = "Le titre",
                            description = "La description"
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episode = platform.episodes().single()

            // Then
            val localization = episode.localizations.single()
            assertEquals(Country.FR, localization.country)
            assertEquals("Le titre", localization.title)
            assertEquals("La description", localization.description)
            assertEquals("https://img/ep-thumb.jpg", episode.image)
        }

        @Test
        @DisplayName("should prefer the episode thumbnail over the series artwork")
        fun `should prefer the episode thumbnail over the series artwork`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode(id = "EP1")),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val image = platform.episodes().single().image

            // Then
            assertEquals("https://img/ep-thumb.jpg", image)
        }

        @Test
        @DisplayName("should fall back to the series artwork when the episode has no image")
        fun `should fall back to the series artwork when the episode has no image`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode(id = "EP1", withImages = false)),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val image = platform.episodes().single().image

            // Then
            assertEquals(
                "https://img/series-SERIES1-tall.jpg",
                image
            )
        }

        @Test
        @DisplayName("should map the release date in UTC")
        fun `should map the release date in UTC`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", premiumAvailableDate = "2026-06-15T18:30:00+02:00")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val releaseDateTime = platform.episodes().single().releaseDateTime

            // Then
            assertEquals("2026-06-15T16:30:00Z", releaseDateTime.toInstant().toString())
        }

        @Test
        @DisplayName("should sanitize the harvested texts")
        fun `should sanitize the harvested texts`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "EP1",
                            seriesTitle = "One\u00A0Piece",
                            title = "Épisode\u00A05",
                            description = "Un\u00A0résumé"
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val harvested = platform.episodes().single()

            // Then
            assertEquals("Épisode 5", harvested.localizations.single().title)
            assertEquals("Un résumé", harvested.localizations.single().description)
            assertEquals("One Piece", harvested.anime.localizations.single().title)
        }

        @Test
        @DisplayName("should fall back to availability_starts when the premium date is blank")
        fun `should fall back to availability starts when the premium date is blank`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(
                            id = "EP1",
                            premiumAvailableDate = "",
                            availabilityStarts = "2026-02-01T10:00:00Z"
                        )
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val releaseDateTime = platform.episodes().single().releaseDateTime

            // Then
            assertEquals("2026-02-01T10:00:00Z", releaseDateTime.toInstant().toString())
        }

        @Test
        @DisplayName("should filter out episodes with a sentinel availability date")
        fun `should filter out episodes with a sentinel availability date`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", premiumAvailableDate = "9998-11-30T00:00:00Z")
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertTrue(episodes.isEmpty())
        }

        @Test
        @DisplayName("should fall back to the series slug when the episode slugs are blank")
        fun `should fall back to the series slug when the episode slugs are blank`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode(id = "EP1", slugTitle = "")),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val url = platform.episodes().single().url

            // Then
            assertEquals("https://www.crunchyroll.com/fr/watch/EP1/one-piece", url)
        }

        @Test
        @DisplayName("should fall back to the series title logo when no artwork is available")
        fun `should fall back to the series title logo when no artwork is available`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode(id = "EP1", withImages = false)),
                    seriesObjects = listOf(series(withImages = false))
                )
            )

            // When
            val image = platform.episodes().single().image

            // Then
            assertTrue(image.contains("keyart/SERIES1-title_logo"))
        }

        @Test
        @DisplayName("should strip the season suffix of the series title and force the season")
        fun `should strip the season suffix of the series title and force the season`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", seriesTitle = "One Piece Saison 2", seasonNumber = 1)
                    ),
                    seriesObjects = listOf(series(title = "One Piece Saison 2"))
                )
            )

            // When
            val episode = platform.episodes().single()

            // Then
            assertEquals("One Piece", episode.anime.localizations.single().title)
            assertEquals(2, episode.season)
        }
    }

    @Nested
    @DisplayName("tests for the show conversion")
    inner class ShowConversionTests {

        @Test
        @DisplayName("should map the series as the platform anime of the episode")
        fun `should map the series as the platform anime of the episode`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", seriesId = "SERIES42", seriesTitle = "Major")
                    ),
                    seriesObjects = listOf(
                        series(
                            id = "SERIES42",
                            title = "Major",
                            description = "A baseball story\\r\\n\\r\\nWith a second paragraph"
                        )
                    )
                )
            )

            // When
            val anime = platform.episodes().single().anime

            // Then
            assertEquals("SERIES42", anime.id)
            assertEquals(Platform.CRUNCHYROLL, anime.platform)
            assertEquals("Major", anime.localizations.single().title)
            assertEquals(
                "With a second paragraph",
                anime.localizations.single().description
            )
        }

        @Test
        @DisplayName("should map the series artwork to the platform anime images")
        fun `should map the series artwork to the platform anime images`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series(id = "SERIES1"))
                )
            )

            // When
            val images = platform.episodes().single().anime.images

            // Then
            assertEquals(
                "https://img/series-SERIES1-tall.jpg",
                images.single { it.type == ImageType.THUMBNAIL }.url
            )
            assertEquals(
                "https://img/series-SERIES1-wide.jpg",
                images.single { it.type == ImageType.BANNER }.url
            )
            assertTrue(
                images.single { it.type == ImageType.TITLE }.url
                    .contains("keyart/SERIES1-title_logo")
            )
        }

        @Test
        @DisplayName("should reuse the same anime instance for episodes of the same series")
        fun `should reuse the same anime instance for episodes of the same series`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(
                        episode(id = "EP1", numberString = "1", number = 1),
                        episode(id = "EP2", numberString = "2", number = 2)
                    ),
                    seriesObjects = listOf(series())
                )
            )

            // When
            val episodes = platform.episodes()

            // Then
            assertSame(episodes[0].anime, episodes[1].anime)
        }

        @Test
        @DisplayName("should return a null anime description when the series has none")
        fun `should return a null anime description when the series has none`() = runTest {
            // Given
            val platform = platform(
                smartHttpClient(
                    browseObjects = listOf(episode()),
                    seriesObjects = listOf(series(description = null))
                )
            )

            // When
            val description = platform.episodes().single().anime.localizations.single().description

            // Then
            assertNull(description)
        }
    }

    private companion object {
        const val TOKEN_RESPONSE = """{"access_token":"test-access-token","expires_in":1800}"""
        const val EMPTY_RESPONSE = """{"data":[]}"""

        /**
         * Wraps the given browse objects into a Crunchyroll `data` JSON body.
         *
         * @param objects The serialized browse objects of the response.
         * @return The JSON body of the response.
         */
        fun browseResponse(vararg objects: String): String =
            """{"data":[${objects.joinToString(",")}]}"""

        /**
         * Wraps the given series objects into a Crunchyroll `data` JSON body.
         *
         * @param objects The serialized series objects of the response.
         * @return The JSON body of the response.
         */
        fun objectsResponse(vararg objects: String): String =
            """{"data":[${objects.joinToString(",")}]}"""

        /**
         * Serializes the images block of a Crunchyroll browse object.
         *
         * @param prefix The prefix of the source URLs of the images.
         * @return The JSON block of the images.
         */
        fun imagesJson(prefix: String): String = """
            "images": {
              "thumbnail": [[{"source":"https://img/$prefix-thumb.jpg","type":"thumbnail","width":800,"height":450}]],
              "poster_tall": [[{"source":"https://img/$prefix-tall.jpg","type":"poster_tall","width":600,"height":900}]],
              "poster_wide": [[{"source":"https://img/$prefix-wide.jpg","type":"poster_wide","width":1600,"height":900}]]
            }
        """.trimIndent()

        /**
         * Serializes an episode browse object of the Crunchyroll discover endpoint.
         *
         * @param id The identifier of the episode.
         * @param seriesId The identifier of the parent series.
         * @param seriesTitle The title of the parent series.
         * @param audioLocale The audio locale of the episode.
         * @param subtitleLocales The subtitle locales of the episode.
         * @param premiumAvailableDate The ISO release date of the episode.
         * @param availabilityStarts The availability start date of the episode.
         * @param seasonNumber The season number of the episode.
         * @param seasonSlugTitle The season slug of the episode.
         * @param seriesSlugTitle The series slug of the episode.
         * @param numberString The raw episode number string.
         * @param number The episode number.
         * @param title The title of the episode.
         * @param slugTitle The slug of the episode.
         * @param durationMs The duration of the episode in milliseconds.
         * @param description The description of the episode.
         * @param matureBlocked Whether the episode is mature blocked.
         * @param identifier The Crunchyroll identifier of the episode.
         * @param withImages Whether the episode carries its own images.
         * @return The JSON payload of the episode browse object.
         */
        fun episode(
            id: String = "EP1",
            seriesId: String = "SERIES1",
            seriesTitle: String = "One Piece",
            audioLocale: String = "fr-FR",
            subtitleLocales: List<String> = listOf("fr-FR", "en-US"),
            premiumAvailableDate: String = "2026-01-10T08:00:00Z",
            availabilityStarts: String? = null,
            seasonNumber: Int? = 1,
            seasonSlugTitle: String? = "one-piece",
            seriesSlugTitle: String? = "one-piece",
            numberString: String = "1",
            number: Int? = 1,
            title: String? = "Episode title",
            slugTitle: String? = "one-piece-episode-1",
            durationMs: Long = 1_420_000,
            description: String? = "Episode description",
            matureBlocked: Boolean = false,
            identifier: String? = null,
            withImages: Boolean = true
        ): String {
            val episodeImages = if (withImages) "\n      ${imagesJson("ep")}," else ""
            val metadataImages = if (withImages) """,
        ${imagesJson("ep-meta")}""" else ""
            return """
                {
                  "id": "$id",
                  "title": ${title?.let { "\"$it\"" } ?: "null"},
                  "description": ${description?.let { "\"$it\"" } ?: "null"},
                  "slug_title": ${slugTitle?.let { "\"$it\"" } ?: "null"},
                  $episodeImages
                  "episode_metadata": {
                    "series_id": "$seriesId",
                    "series_title": "$seriesTitle",
                    "series_slug_title": ${seriesSlugTitle?.let { "\"$it\"" } ?: "null"},
                    "audio_locale": "$audioLocale",
                    "subtitle_locales": [${subtitleLocales.joinToString(", ") { "\"$it\"" }}],
                    "premium_available_date": "$premiumAvailableDate",
                    "availability_starts": ${availabilityStarts?.let { "\"$it\"" } ?: "null"},
                    "season_id": "SEASON1",
                    "season_number": ${seasonNumber ?: "null"},
                    "season_slug_title": ${seasonSlugTitle?.let { "\"$it\"" } ?: "null"},
                    "episode": "$numberString",
                    "episode_number": ${number ?: "null"},
                    "title": ${title?.let { "\"$it\"" } ?: "null"},
                    "slug_title": ${slugTitle?.let { "\"$it\"" } ?: "null"},
                    "duration_ms": $durationMs,
                    "description": ${description?.let { "\"$it\"" } ?: "null"},
                    "mature_blocked": $matureBlocked,
                    "identifier": ${identifier?.let { "\"$it\"" } ?: "null"}$metadataImages
                  }
                }
            """.trimIndent()
        }

        /**
         * Serializes a series browse object of the Crunchyroll CMS objects endpoint.
         *
         * @param id The identifier of the series.
         * @param title The title of the series.
         * @param description The description of the series.
         * @param withImages Whether the series carries images.
         * @return The JSON payload of the series browse object.
         */
        fun series(
            id: String = "SERIES1",
            title: String = "One Piece",
            description: String? = "Tagline\\r\\n\\r\\nPirates summary",
            withImages: Boolean = true
        ): String {
            val images = if (withImages) "${imagesJson("series-$id")}," else ""
            return """
                {
                  "id": "$id",
                  "title": "$title",
                  "slug_title": "one-piece",
                  "description": ${description?.let { "\"$it\"" } ?: "null"},
                  $images
              "series_metadata": {
                "id": "$id",
                "title": "$title",
                "slug_title": "one-piece",
                "description": ${description?.let { "\"$it\"" } ?: "null"}
              }
            }
        """.trimIndent()
        }
    }
}
