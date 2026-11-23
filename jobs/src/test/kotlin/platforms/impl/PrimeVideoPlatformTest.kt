package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.platforms.models.PlatformEpisode
import fr.shikkanime.models.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.*
import kotlin.time.Duration

@ExtendWith(MockKExtension::class)
@DisplayName("tests for PrimeVideoPlatform")
class PrimeVideoPlatformTest {

    /**
     * Builds a [SmartHttpClient] backed by a mock engine routing the Prime Video endpoints.
     *
     * The detail endpoint answers with the page payload registered for the requested compact
     * identifier and the `getDetailWidgets` endpoint with the widget payloads registered per
     * widget token.
     *
     * @param pages The page payloads indexed by compact detail identifier.
     * @param widgets The widget payloads indexed by widget token.
     * @param onRequest Callback observing every outgoing request before the answer is produced.
     * @return The [SmartHttpClient] under test.
     * @see [SmartHttpClient]
     */
    private fun smartHttpClient(
        pages: Map<String, String> = emptyMap(),
        widgets: Map<String, String> = emptyMap(),
        onRequest: (HttpRequestData) -> Unit = {}
    ): SmartHttpClient =
        SmartHttpClient(
            client = HttpClient(
                MockEngine { request ->
                    onRequest(request)
                    val url = request.url.toString()
                    val body = when {
                        "/api/getDetailWidgets" in url -> {
                            val token = WIDGET_TOKEN_REGEX.find(url)?.groupValues?.get(1)
                            widgets[token] ?: EMPTY_WIDGETS
                        }
                        "/detail/" in url -> {
                            val compactId = url.substringAfter("/detail/")
                                .substringBefore("?")
                            pages[compactId] ?: EMPTY_PAGE
                        }
                        else -> EMPTY_PAGE
                    }
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            },
            responseDumpDir = File("build/test-http-dumps")
        )

    /**
     * Builds the [PrimeVideoPlatform] under test.
     *
     * @param client The [SmartHttpClient] serving the Prime Video payloads.
     * @param countries The configured target countries of the platform.
     * @param showIds The tracked show identifiers of the platform.
     * @return The [PrimeVideoPlatform] under test.
     * @see [PrimeVideoPlatform]
     */
    private fun platform(
        client: SmartHttpClient,
        countries: Set<Country> = setOf(Country.FR),
        showIds: Set<String> = setOf(SHOW_ID)
    ): PrimeVideoPlatform =
        PrimeVideoPlatform(
            client = client,
            ttl = Duration.ZERO,
            countries = countries,
            showIds = showIds
        )

    /**
     * Harvests the episodes of the platform, leaving the change flag aside.
     *
     * @param client The [SmartHttpClient] serving the Prime Video payloads.
     * @param countries The configured target countries of the platform.
     * @param showIds The tracked show identifiers of the platform.
     * @return The harvested [PlatformEpisode] list.
     * @see [PrimeVideoPlatform.fetchLatestEpisodes]
     */
    private suspend fun harvest(
        client: SmartHttpClient,
        countries: Set<Country> = setOf(Country.FR),
        showIds: Set<String> = setOf(SHOW_ID)
    ): List<PlatformEpisode> =
        platform(client, countries, showIds).fetchLatestEpisodes().data

    /**
     * Builds the JSON payload of a series detail page with two inline episodes.
     *
     * @param extraEpisode The key of an extra episode fixture to append, `null` for none.
     * @return The serialized page payload.
     */
    private fun seriesPage(extraEpisode: String? = null): String =
        """{"body":{"atf":{"state":${atfState(seasons = "{}")}},"btf":{"state":${btfState(
            episodes = episodeEntities("episode1", "episode2") +
                (extraEpisode?.let { "," + episodeEntities(it) }.orEmpty()),
            selves = selves("episode1", "episode2") +
                (extraEpisode?.let { "," + selves(it) }.orEmpty()),
            metadatas = metadatas("episode1", "episode2") +
                (extraEpisode?.let { "," + metadatas(it) }.orEmpty())
        )}}}}"""

    /**
     * Builds the JSON payload of a series detail page listing two seasons.
     *
     * @return The serialized page payload of the first season.
     */
    private fun seriesPageWithSeasons(): String =
        """{"body":{"atf":{"state":${atfState(
            seasons = """{"key":[${seasonEntry("amzn1.dv.gti.season1", SHOW_ID, 1, true)},""" +
                """${seasonEntry("amzn1.dv.gti.season2", SEASON_2_ID, 2, false)}]}"""
        )}},"btf":{"state":${btfState(
            episodes = episodeEntities("episode1"),
            selves = selves("episode1"),
            metadatas = metadatas("episode1")
        )}}}}"""

    /**
     * Builds the JSON payload of the second season detail page.
     *
     * @return The serialized page payload.
     */
    private fun seasonTwoPage(): String =
        """{"body":{"atf":{"state":${atfState(
            pageTitleId = "amzn1.dv.gti.season2",
            seasons = "{}"
        )}},"btf":{"state":${btfState(
            pageTitleId = "amzn1.dv.gti.season2",
            episodes = episodeEntities("season2episode"),
            selves = selves("season2episode", number = 2),
            metadatas = metadatas("season2episode")
        )}}}}"""

    /**
     * Builds the JSON payload of a series detail page with one paginated episode card.
     *
     * @return The serialized page payload.
     */
    private fun seriesPageWithPagination(): String =
        """{"body":{"atf":{"state":${atfState(seasons = "{}")}},"btf":{"state":${btfState(
            episodes = episodeEntities("episode1"),
            selves = selves("episode1"),
            metadatas = metadatas("episode1"),
            pagination = """[{"token":"NEXT_PAGE_TOKEN","tokenType":"NextPage","text":{"string":"Page suivante"}}]"""
        )}}}}"""

    /**
     * Builds the JSON payload of a `getDetailWidgets` response with two episode cards.
     *
     * @return The serialized widget payload.
     */
    private fun widgetsPage(): String =
        """{"widgets":{"episodeList":{"episodes":[${widgetItem("episode2")},""" +
            """${widgetItem("episode3", number = 3)}],"actions":{"pagination":[]}}}}"""

    /**
     * Builds the JSON payload of a movie detail page.
     *
     * @return The serialized page payload.
     */
    private fun moviePage(): String =
        """{"body":{"atf":{"state":${atfState(
            pageTitleId = "amzn1.dv.gti.movie1",
            header = movieHeader(),
            seasons = "{}"
        )}},"btf":{"state":${btfState(
            pageTitleId = "amzn1.dv.gti.movie1",
            episodes = "",
            selves = "",
            metadatas = ""
        )}}}}"""

    private fun atfState(
        pageTitleId: String = "amzn1.dv.gti.season1",
        header: String = seriesHeader(),
        seasons: String
    ): String =
        """{"pageTitleId":"$pageTitleId","detail":{"headerDetail":{"$pageTitleId":$header}},"seasons":$seasons}"""

    private fun seriesHeader(): String =
        """{"title":"Boruto - Naruto Next Generations","parentTitle":"Boruto - Naruto Next Generations",""" +
            """"synopsis":"La suite de Naruto.","entityType":"TV Show","titleType":"season","seasonNumber":1,""" +
            """"images":{"covershot":"https://m.media-amazon.com/images/covershot.jpg",""" +
            """"packshot":"https://m.media-amazon.com/images/packshot.jpg",""" +
            """"titleLogo":"https://m.media-amazon.com/images/titlelogo.png"}}"""

    private fun movieHeader(): String =
        """{"title":"One Piece Film : Red","synopsis":"Le film.","entityType":"Movie","titleType":"movie",""" +
            """"duration":6637,"releaseDate":"10 août 2022",""" +
            """"audioTracks":["Français","日本語"],"subtitles":["Français"],""" +
            """"images":{"covershot":"https://m.media-amazon.com/images/movie.png"}}"""

    private fun seasonEntry(
        seasonId: String,
        link: String,
        number: Int,
        selected: Boolean
    ): String =
        """{"seasonId":"$seasonId","seasonLink":"/-/fr/detail/$link?ref_=atv_dp_season_select_s$number",""" +
            """"displayName":"Saison $number","sequenceNumber":$number,"isSelected":$selected}"""

    private fun btfState(
        pageTitleId: String = "amzn1.dv.gti.season1",
        episodes: String,
        selves: String,
        metadatas: String,
        pagination: String = "[]"
    ): String =
        """{"pageTitleId":"$pageTitleId","detail":{"detail":{$episodes}},"self":{$selves},""" +
            """"metadata":{$metadatas},"episodeList":{"actions":{"pagination":$pagination}}}"""

    private fun episodeEntities(vararg keys: String): String =
        keys.joinToString(",") { "\"amzn1.dv.gti.$it\":${episodeEntity(it)}" }

    private fun selves(vararg keys: String, number: Int? = null): String =
        keys.joinToString(",") { key ->
            val episodeNumber = number ?: key.removePrefix("episode").toIntOrNull() ?: 1
            """"amzn1.dv.gti.$key":{"compactGTI":"0${key.uppercase()}COMPACTID000000000000",""" +
                """"gti":"amzn1.dv.gti.$key","isLaunched":true,""" +
                """"link":"/-/fr/detail/0${key.uppercase()}COMPACTID000000000000?ref_=atv_dp",""" +
                """"sequenceNumber":$episodeNumber,"titleType":"episode"}"""
        }

    private fun metadatas(vararg keys: String): String =
        keys.joinToString(",") { key ->
            val metadata = when (key) {
                "upcomingEpisode" ->
                    """"traits":["Upcoming"],"upcomingSynopsis":"Bientôt disponible.""""
                else -> """"traits":[]"""
            }
            """"amzn1.dv.gti.$key":{$metadata}"""
        }

    private fun episodeEntity(key: String): String = when (key) {
        "germanOnlyEpisode" -> episodeJson(
            key, number = 3, audio = """["Deutsch","日本語"]""",
            subtitles = """["Deutsch"]"""
        )
        "subtitleOnlyEpisode" -> episodeJson(
            key, number = 3, audio = """["日本語"]""",
            subtitles = """["Français [CC]"]"""
        )
        "upcomingEpisode" -> episodeJson(key, number = 4)
        "badDateEpisode" -> episodeJson(key, number = 5, releaseDate = "date inconnue")
        "germanEpisode" -> episodeJson(
            key, number = 4, audio = """["Deutsch","日本語"]""",
            subtitles = """["Deutsch [CC]"]"""
        )
        "season2episode" -> episodeJson(key, number = 2, releaseDate = "4 juin 2018")
        else -> episodeJson(
            key,
            number = key.removePrefix("episode").toIntOrNull() ?: 1,
            releaseDate = if (key == "episode2") "4 juin 2018" else "28 mai 2018"
        )
    }

    private fun episodeJson(
        key: String,
        number: Int,
        releaseDate: String = "28 mai 2018",
        audio: String = """["Français","Français [Audiodescription]","日本語"]""",
        subtitles: String = """["Français [CC]","English [auto]"]"""
    ): String =
        """{"title":"Episode $number","synopsis":"Synopsis $number.","titleType":"episode",""" +
            """"episodeNumber":$number,"duration":1430,"releaseDate":"$releaseDate",""" +
            """"audioTracks":$audio,"subtitles":$subtitles,""" +
            """"images":{"covershot":"https://m.media-amazon.com/images/S/pv-target-images/$key.png"}}"""

    private fun widgetItem(key: String, number: Int = 2): String =
        """{"detail":{"catalogId":"amzn1.dv.gti.$key","titleType":"episode","title":"Episode $number",""" +
            """"synopsis":"Synopsis $number.","episodeNumber":$number,"duration":1430,""" +
            """"releaseDate":"4 juin 2018","audioTracks":["Français","日本語"],""" +
            """"subtitles":["Français [CC]"],"images":{"covershot":"https://m.media-amazon.com/images/$key.png"}},""" +
            """"self":{"compactGTI":"0${key.uppercase()}COMPACTID000000000000",""" +
            """"link":"/-/fr/detail/0${key.uppercase()}COMPACTID000000000000?ref_=atv_dp",""" +
            """"sequenceNumber":$number,"isLaunched":true},"metadata":{"traits":[]}}"""

    @Nested
    @DisplayName("series harvest")
    inner class SeriesHarvest {

        @Test
        @DisplayName("should map the episodes of a season page")
        fun shouldMapTheEpisodesOfASeasonPage() = runTest {
            // Given
            val client = smartHttpClient(pages = mapOf(SHOW_ID to seriesPage()))

            // When
            val episodes = harvest(client)

            // Then
            assertEquals(2, episodes.size)
            val episode = episodes.single { it.number == 1 }
            assertEquals(SHOW_ID, episode.anime.id)
            assertEquals(Platform.PRIME_VIDEO, episode.platform)
            assertEquals(compactId("episode1"), episode.id)
            assertEquals(1, episode.season)
            assertEquals(EpisodeType.EPISODE, episode.episodeType)
            assertEquals(1430L, episode.duration)
            assertEquals(
                "https://www.primevideo.com/-/fr/detail/${compactId("episode1")}?autoplay=1&t=0",
                episode.url
            )
            assertEquals(
                listOf(Locale.FR_FR, Locale.JA_JP).toSet(),
                episode.audioLocales.toSet()
            )
            assertFalse(episode.uncensored)
            assertEquals(
                "https://m.media-amazon.com/images/S/pv-target-images/episode1.png",
                episode.image
            )
            assertEquals(
                ZonedDateTime.of(2018, 5, 28, 0, 0, 0, 0, ZoneOffset.UTC),
                episode.releaseDateTime
            )
            assertEquals("Episode 1", episode.localizations.single().title)
            assertEquals(
                "Synopsis 1.",
                episode.localizations.single().description
            )
        }

        @Test
        @DisplayName("should map the show images and localization")
        fun shouldMapTheShowImagesAndLocalization() = runTest {
            // Given
            val client = smartHttpClient(pages = mapOf(SHOW_ID to seriesPage()))

            // When
            val episodes = harvest(client)

            // Then
            val anime = episodes.first().anime
            assertEquals(
                listOf(
                    ImageType.BANNER to "https://m.media-amazon.com/images/covershot.jpg",
                    ImageType.THUMBNAIL to "https://m.media-amazon.com/images/packshot.jpg",
                    ImageType.TITLE to "https://m.media-amazon.com/images/titlelogo.png"
                ),
                anime.images.map { it.type to it.url }
            )
            val localization = anime.localizations.single()
            assertEquals(Country.FR, localization.country)
            assertEquals(Locale.FR_FR, localization.locale)
            assertEquals("Boruto - Naruto Next Generations", localization.title)
            assertEquals("La suite de Naruto.", localization.description)
        }

        @Test
        @DisplayName("should harvest the episodes of every season")
        fun shouldHarvestTheEpisodesOfEverySeason() = runTest {
            // Given
            val client = smartHttpClient(
                pages = mapOf(
                    SHOW_ID to seriesPageWithSeasons(),
                    SEASON_2_ID to seasonTwoPage()
                )
            )

            // When
            val episodes = harvest(client)

            // Then
            assertEquals(setOf(1, 2), episodes.map(PlatformEpisode::season).toSet())
            assertEquals(2, episodes.size)
            assertEquals(
                2,
                episodes.single { it.season == 2 }.number
            )
        }

        @Test
        @DisplayName("should paginate the episode cards through getDetailWidgets")
        fun shouldPaginateTheEpisodeCardsThroughGetDetailWidgets() = runTest {
            // Given
            val requestedUrls = mutableListOf<String>()
            val client = smartHttpClient(pages = mapOf(SHOW_ID to seriesPageWithPagination()))
            val platform = platform(client)
            platform.widgetClient = smartHttpClient(
                widgets = mapOf("NEXT_PAGE_TOKEN" to widgetsPage()),
                onRequest = { requestedUrls += it.url.toString() }
            )

            // When
            val episodes = platform.fetchLatestEpisodes().data

            // Then
            assertEquals(3, episodes.size)
            assertTrue(requestedUrls.any { "/api/getDetailWidgets" in it })
            assertTrue(
                requestedUrls.any {
                    "/api/getDetailWidgets" in it && "titleID=" in it
                }
            )
        }

        @Test
        @DisplayName("should report no change when the payloads are identical")
        fun shouldReportNoChangeWhenThePayloadsAreIdentical() = runTest {
            // Given
            val client = smartHttpClient(pages = mapOf(SHOW_ID to seriesPage()))
            val platform = platform(client)

            // When
            val first = platform.fetchLatestEpisodes()
            val second = platform.fetchLatestEpisodes()

            // Then
            assertTrue(first.hasChanged)
            assertFalse(second.hasChanged)
            assertEquals(first.data, second.data)
        }
    }

    @Nested
    @DisplayName("movie harvest")
    inner class MovieHarvest {

        @Test
        @DisplayName("should map a movie as a single film episode")
        fun shouldMapAMovieAsASingleFilmEpisode() = runTest {
            // Given
            val client = smartHttpClient(pages = mapOf(SHOW_ID to moviePage()))

            // When
            val episodes = harvest(client)

            // Then
            assertEquals(1, episodes.size)
            val episode = episodes.single()
            assertEquals(EpisodeType.FILM, episode.episodeType)
            assertEquals(1, episode.season)
            assertEquals(1, episode.number)
            assertEquals(6637L, episode.duration)
            assertEquals(
                "One Piece Film : Red",
                episode.localizations.single().title
            )
        }
    }

    @Nested
    @DisplayName("release filters")
    inner class ReleaseFilters {

        @Test
        @DisplayName("should drop episodes without audio nor subtitle for the country")
        fun shouldDropEpisodesWithoutAudioNorSubtitleForTheCountry() = runTest {
            // Given
            val client = smartHttpClient(
                pages = mapOf(SHOW_ID to seriesPage(extraEpisode = "germanOnlyEpisode"))
            )

            // When
            val episodes = harvest(client)

            // Then
            assertEquals(listOf(1, 2), episodes.map { it.number })
        }

        @Test
        @DisplayName("should keep an episode subtitled but not dubbed in the country language")
        fun shouldKeepAnEpisodeSubtitledButNotDubbedInTheCountryLanguage() = runTest {
            // Given
            val client = smartHttpClient(
                pages = mapOf(SHOW_ID to seriesPage(extraEpisode = "subtitleOnlyEpisode"))
            )

            // When
            val episodes = harvest(client)

            // Then
            val episode = episodes.single { it.number == 3 }
            assertEquals(listOf(Locale.JA_JP), episode.audioLocales)
        }

        @Test
        @DisplayName("should drop upcoming and unlaunched episodes")
        fun shouldDropUpcomingAndUnlaunchedEpisodes() = runTest {
            // Given
            val client = smartHttpClient(
                pages = mapOf(SHOW_ID to seriesPage(extraEpisode = "upcomingEpisode"))
            )

            // When
            val episodes = harvest(client)

            // Then
            assertEquals(2, episodes.size)
        }

        @Test
        @DisplayName("should keep an episode with an unparseable release date out")
        fun shouldKeepAnEpisodeWithAnUnparseableReleaseDateOut() = runTest {
            // Given
            val client = smartHttpClient(
                pages = mapOf(SHOW_ID to seriesPage(extraEpisode = "badDateEpisode"))
            )

            // When
            val episodes = harvest(client)

            // Then
            assertEquals(2, episodes.size)
        }

        @Test
        @DisplayName("should resolve the German audio locale for the German distribution")
        fun shouldResolveTheGermanAudioLocaleForTheGermanDistribution() = runTest {
            // Given
            val client = smartHttpClient(
                pages = mapOf(SHOW_ID to seriesPage(extraEpisode = "germanEpisode"))
            )

            // When
            val episodes = harvest(client, countries = setOf(Country.DE))

            // Then
            val episode = episodes.single { it.number == 4 }
            assertEquals(listOf(Locale.DE_DE, Locale.JA_JP).toSet(), episode.audioLocales.toSet())
            assertEquals(Country.DE, episode.localizations.single().country)
        }
    }

    private companion object {
        const val SHOW_ID = "0SHOWCOMPACTID000000000000"
        const val SEASON_2_ID = "0SEASON2COMPACTID00000000000"

        val WIDGET_TOKEN_REGEX = Regex("""widgetToken%22%3A%22([^%]+)""")

        fun compactId(key: String): String =
            "0${key.uppercase()}COMPACTID000000000000"

        const val EMPTY_PAGE = """{"body":{}}"""
        const val EMPTY_WIDGETS = """{"widgets":{}}"""
    }
}
