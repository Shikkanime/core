package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.SmartResponse
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.jobs.platforms.exceptions.MissingAudioLocaleException
import fr.shikkanime.jobs.platforms.models.*
import fr.shikkanime.jobs.platforms.sanitizeText
import fr.shikkanime.ktor.defaultJson
import fr.shikkanime.models.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private const val BASE_URL = "https://www.primevideo.com"
private const val CLIENT_VERSION = "1.0.132106.0"
private const val MAX_WIDGET_PAGES = 40

// Amazon image CDN operations chained as `._<op>_<op>_` before the file extension:
// `_UR<w>,<h>_` resizes to the exact box and `_CR<x>,<y>,<w>,<h>_` crops a region.
// Prime Video detail payloads only expose landscape rasters (covershot/heroshot,
// 16:9) and 4:3 rasters (packshot/titleshot), so the canonical ImageType dimensions
// are reached by cropping the 4:3 sources instead of stretching them. Native 2:3
// posters (CDN_THUMBNAIL) are only resized, never cropped.
private const val CDN_BANNER = "UR1920,1080"
private const val CDN_BANNER_FROM_4X3 = "UR1920,1440_CR0,180,1920,1080"
private const val CDN_THUMBNAIL = "UR1560,2340"
private const val CDN_THUMBNAIL_FROM_4X3 = "UR3120,2340_CR780,0,1560,2340"

/**
 * Prime Video portrait posters (`poster2x3` assets, 2000x3000) curated in the production
 * database, indexed by compact show identifier.
 *
 * The detail page payload only exposes landscape (covershot/heroshot) and 4:3
 * (packshot/titleshot) rasters for the main title; the dedicated 2:3 poster is served
 * through authenticated browse APIs and variant carousels, so it cannot be harvested
 * automatically. These URLs are migrated from the production `attachment` table and
 * normalized to [ImageType.THUMBNAIL] dimensions through [cdnImage]. Shows absent from
 * this map fall back to the center-cropped packshot.
 */
private val DEFAULT_THUMBNAILS = mapOf(
    "0FHCA8284A4JP1BGB6EZM1UHXM" to
        "https://images-eu.ssl-images-amazon.com/images/S/pv-target-images/" +
            "14e3ba0d7a47b0d39c9734ba7d16ea233ed6b16936cb6d8c45a34cf8e02a1e6e.jpg",
    "0G5HKRN0W1F71INMKBJE3UJDL" to
        "https://m.media-amazon.com/images/S/pv-target-images/" +
            "0903a8ee4fc60a17cc409aa83ec0b011852310a1c034864e05c459d532ea5f7b.jpg",
    "0GMSR5WMNJ6EMYQRQ6Y2587WAF" to
        "https://images-eu.ssl-images-amazon.com/images/S/pv-target-images/" +
            "3e7d1cac716822d6c04c861587d13fb7e34257188ea0661515ffe737a11c21bd.jpg",
    "0J35D20PPSSNACER7Z6BEAHQFW" to
        "https://images-eu.ssl-images-amazon.com/images/S/pv-target-images/" +
            "a63a0079db34affeb26672ff67b1847700a766b61acd54702f2d54109af16f7f.png",
    "0LW570H4NYAE6XSCD8I7KZMVNH" to
        "https://images-eu.ssl-images-amazon.com/images/S/pv-target-images/" +
            "4f82235783e63fa70ae6e3dfed772e75e78e3911625d504d9effaab4f0c04ad9.png",
    "0MRWXUDE3L303XFEJRNWB20S3O" to
        "https://images-eu.ssl-images-amazon.com/images/S/pv-target-images/" +
            "dc699c57fc921d616ba132ed6c082fbdbd9106e02516db611d2c9585c9624cbe.jpg",
    "0SMLN9RBRDL719Y0K5J00JHJ3G" to
        "https://m.media-amazon.com/images/S/pv-target-images/" +
            "b2874eea83724714a98402fbf1048b2f27430a2c09ebea33841ef3637df7e51e.jpg"
)

// The Prime Video frontend rejects requests carrying the default HTTP client user agent with
// redirect loops on its API endpoints; a desktop browser agent is required to negotiate the
// JSON representations of the detail pages and widget payloads.
private const val USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

/**
 * Prime Video page identifiers of the anime catalog harvested by this platform.
 *
 * The legacy implementation harvested a configured simulcast list; the new architecture has no
 * platform configuration store, so the catalog migrated from the production database is carried
 * here as the default set. Operators extend it by passing [PrimeVideoPlatform.showIds].
 */
private val DEFAULT_SHOW_IDS = setOf(
    "0FDNZG74Z3YIIIQ4HPWX966TO2",
    "0FHCA8284A4JP1BGB6EZM1UHXM",
    "0FYBTVJ1JB731K9U0XDE7U20F1",
    "0G5HKRN0W1F71INMKBJE3UJDL",
    "0GMSR5WMNJ6EMYQRQ6Y2587WAF",
    "0J35D20PPSSNACER7Z6BEAHQFW",
    "0JHJJJYBCOBY4LZFYHGU6JJDWX",
    "0K39YOLXXIAWD38NPGL4W9D6F2",
    "0KF5WYD9EMI7DIYOVTH56ANAN2",
    "0KFD79CBVX90IWRIG5NLAGJ7R1",
    "0LT5I796GDNEXFC0NA9A653MJR",
    "0LW570H4NYAE6XSCD8I7KZMVNH",
    "0MNO6WKB8I3016041RSOV5NKH4",
    "0MRWXUDE3L303XFEJRNWB20S3O",
    "0MWCE6QKWI8NTMW0R53A91T2HY",
    "0NDRDUXY9Z9S36DIFNZY6X3HD4",
    "0NV4FUWKV9BQN8VDIIH1DWEEHH",
    "0NYLPINCP0E62I4RU0VCLOK7EE",
    "0O1YMOCTYRHW1QWHTGOS6YASPJ",
    "0OTYB5HOFEGL63J0E2NDCL4UCM",
    "0QA3P8T387P0WAV0KXUYBWDDYR",
    "0QN9ZXJ935YBTNK8U9FV5OAX5B",
    "0QTHN6BJ67YJNWF5JMME3RK5D7",
    "0S1DSDV3YPNM8W8GZQK8FJTZ9H",
    "0S9PUSDV9DM67VQNLIRJJV5N9F",
    "0SMLN9RBRDL719Y0K5J00JHJ3G",
    "0TKRFV2FB8U18G5WS16GFOXNG2",
    "0TRGLGKLJS99OYM9647IM6Y1N0",
    "0U1JU7SSQMNO2O1760K77BR184"
)

/**
 * Streaming platform connector for Prime Video.
 *
 * This implementation scrapes the public Prime Video web detail pages: every tracked show is
 * fetched once per page language as a JSON payload (`dvWebAppClientVersion` negotiation), its
 * seasons are resolved through their `seasonLink` pages, and the episode cards past the inline
 * page are paginated through the `getDetailWidgets` endpoint. Movies are harvested as a single
 * [EpisodeType.FILM] release; series produce one [EpisodeType.EPISODE] per card.
 *
 * A release is kept only when it is dubbed or subtitled in the language of the target country,
 * upcoming and unlaunched cards are rejected, and every resolvable audio track becomes an
 * [Locale] of the release, so a subtitled-only episode still carries its Japanese audio track.
 *
 * @param client The [SmartHttpClient] instance used to execute the cached HTTP requests of
 * the platform, the detail pages included. The widget pagination calls go through a dedicated
 * [SmartHttpClient] whose Ktor client never follows redirects: the endpoint's bot check sends
 * the shared client into a redirect loop.
 * @param ttl The cache time-to-live [Duration] for detail and widget responses. It defaults to
 * one hour: Prime Video detail pages embed per-request tokens, so the HTTP change detection
 * almost always reports a change and the in-memory episode comparison is what actually gates
 * the harvest.
 * @param countries The target countries enabled for this platform connector.
 * @param showIds The Prime Video detail identifiers of the tracked catalog.
 * @see [StreamingPlatform]
 * @see [PlatformEpisode]
 * @see [PlatformAnime]
 * @see [Platform.PRIME_VIDEO]
 */
@Single(binds = [StreamingPlatform::class])
class PrimeVideoPlatform(
    private val client: SmartHttpClient,
    private val ttl: Duration = 1.hours,
    override val countries: Set<Country> = setOf(Country.FR),
    private val showIds: Set<String> = DEFAULT_SHOW_IDS,
    private val thumbnails: Map<String, String> = DEFAULT_THUMBNAILS
) : StreamingPlatform {
    override val platform: Platform = Platform.PRIME_VIDEO

    private val logger = LoggerFactory.getLogger()
    private var cachedEpisodes: List<PlatformEpisode> = emptyList()
    private val cachedEpisodesByCountry = mutableMapOf<Country, List<PlatformEpisode>>()
    private val releaseDateFormatters = ConcurrentHashMap<String, List<DateTimeFormatter>>()

    // The widget endpoint sits behind a bot check that redirects the shared Ktor client into a
    // redirect loop; disabling redirect following negotiates the JSON payload directly, and the
    // SmartHttpClient wrapper keeps the caching, change detection and stale fallback.
    internal var widgetClient: SmartHttpClient = SmartHttpClient(
        client = HttpClient(OkHttp) {
            followRedirects = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            install(ContentNegotiation) {
                json(defaultJson)
            }
        }
    )

    /**
     * Retrieves the episodes of the tracked Prime Video catalog across configured countries.
     *
     * For every country declared in [countries], every show of [showIds] is fetched in the page
     * language of the country, its seasons and paginated episode cards are harvested, and valid
     * releases are mapped into [PlatformEpisode] instances.
     *
     * In-memory cache is returned while [SmartHttpClient] reports that the remote payloads have
     * not changed. A payload change that leaves the harvested episodes untouched, like a reshuffle
     * of the embedded state, is not reported as a change.
     *
     * @return A [SmartResponse] carrying the normalized [PlatformEpisode] objects and the change
     * flag of the harvested episodes.
     * @see [SmartResponse]
     * @see [PlatformEpisode]
     */
    override suspend fun fetchLatestEpisodes(): SmartResponse<List<PlatformEpisode>> {
        if (countries.isEmpty() || showIds.isEmpty())
            return SmartResponse(emptyList(), hasChanged = false)

        var hasChanged = false

        val episodesByCountry = countries.flatMap { country ->
            val (episodes, changed) = fetchEpisodesForCountry(country)
            if (changed)
                hasChanged = true
            episodes
        }

        if (hasChanged)
            cachedEpisodes = episodesByCountry

        return SmartResponse(cachedEpisodes, hasChanged)
    }

    /**
     * Fetches and maps the Prime Video releases of the tracked catalog for a specific country.
     *
     * Every show is harvested in the page language of the country; countries sharing a language
     * share the same cached HTTP payloads but produce their own localizations.
     *
     * @param country Target country distribution.
     * @return The mapped [PlatformEpisode] list of the country and whether the payload changed.
     * @see [Country]
     * @see [PlatformEpisode]
     */
    private suspend fun fetchEpisodesForCountry(
        country: Country
    ): Pair<List<PlatformEpisode>, Boolean> {
        val language = pageLanguage(country)
        var payloadChanged = false

        val shows = showIds.mapNotNull { showId ->
            try {
                val (show, changed) = harvestShow(language, showId)
                payloadChanged = payloadChanged || changed
                show
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warning("Skipping Prime Video show $showId ($language): ${e.message}")
                null
            }
        }

        val cached = cachedEpisodesByCountry[country]
        if (!payloadChanged && cached != null)
            return cached to false

        val episodes = shows.flatMap { show -> convertShow(show, country, language) }

        // The embedded state may reshuffle its entries without changing the episodes they carry:
        // the change flag must reflect the harvested episodes, not the raw payload order.
        if (cached != null && episodes.hasSameContentAs(cached))
            return cached to false

        cachedEpisodesByCountry[country] = episodes
        return episodes to true
    }

    /**
     * Harvests the raw catalog data of one show: its header, its seasons and every episode card.
     *
     * The entry detail page is fetched first; when the show is a series, every other season is
     * resolved through its `seasonLink` page and the episode cards past the inline page are
     * paginated through the `getDetailWidgets` endpoint.
     *
     * @param language The page language of the country, used in the detail URL and the headers.
     * @param showId The Prime Video detail identifier of the show.
     * @return The harvested [PvShowData], or `null` when the page carries no usable state, and
     * whether any fetched payload changed.
     * @see [PvShowData]
     * @see [fetchPage]
     */
    private suspend fun harvestShow(
        language: String,
        showId: String
    ): Pair<PvShowData?, Boolean> {
        val entry = fetchPage(language, showId)
        var changed = entry.hasChanged

        val atf = entry.data.body.atf.state
        val btf = entry.data.body.btf.state
        if (atf == null || btf == null) {
            logger.warning("Prime Video page $showId ($language) carries no state")
            return null to changed
        }

        val pageTitleId = atf.pageTitleId ?: return null to changed
        val header = atf.detail.headerDetail[pageTitleId]
            ?: return null to changed

        val showImages = header.images
        val baseShow = PvShowData(
            compactId = showId,
            title = header.title,
            parentTitle = header.parentTitle,
            synopsis = header.synopsis,
            bannerImage = showImages?.covershot
                ?.let { cdnImage(it, CDN_BANNER) },
            thumbnailImage = thumbnails[showId]
                ?.let { cdnImage(it, CDN_THUMBNAIL) }
                ?: listOfNotNull(showImages?.packshot, showImages?.titleshot)
                    .firstOrNull(String::isNotBlank)
                    ?.let { cdnImage(it, CDN_THUMBNAIL_FROM_4X3) },
            logoImage = showImages?.titleLogo?.takeIf(String::isNotBlank),
            isMovie = header.entityType == "Movie" || header.titleType == "movie"
        )

        if (baseShow.isMovie) {
            val card = header.toEpisodeCard(
                language = language,
                gti = pageTitleId,
                compactGti = showId,
                fallbackUrl = "$BASE_URL/-/$language/detail/$showId?autoplay=1&t=0"
            )
            return baseShow.copy(seasons = listOf(PvSeasonData(1, listOf(card)))) to changed
        }

        val seasonEntries = atf.seasons.values.flatten()
            .sortedBy { it.sequenceNumber ?: Int.MAX_VALUE }

        if (seasonEntries.isEmpty()) {
            val (cards, seasonChanged) = harvestSeasonEpisodes(language, pageTitleId, btf)
            changed = changed || seasonChanged
            return baseShow.copy(
                seasons = listOf(PvSeasonData(1, cards))
            ) to changed
        }

        var entryPageConsumed = false
        val seasons = seasonEntries.mapNotNull { seasonEntry ->
            val seasonId = seasonEntry.seasonId ?: return@mapNotNull null
            val seasonNumber = seasonEntry.sequenceNumber ?: 1
            val seasonBtf: PrimeVideoState
            val seasonPageTitleId: String

            if (seasonId == pageTitleId || seasonEntry.isSelected == true) {
                seasonBtf = btf
                seasonPageTitleId = pageTitleId
                entryPageConsumed = true
            } else {
                val link = seasonEntry.seasonLink ?: return@mapNotNull null
                val compactId = link.substringAfter("/detail/").substringBefore("?")
                val seasonPage = fetchPage(language, compactId)
                changed = changed || seasonPage.hasChanged
                seasonBtf = seasonPage.data.body.btf.state
                    ?: return@mapNotNull null
                seasonPageTitleId = seasonPage.data.body.atf.state?.pageTitleId
                    ?: seasonId
            }

            val (cards, seasonChanged) = harvestSeasonEpisodes(
                language, seasonPageTitleId, seasonBtf
            )
            changed = changed || seasonChanged
            PvSeasonData(seasonNumber, cards)
        }

        // The entry page always describes one season: when the seasons selector does not list it,
        // its episodes would otherwise be lost.
        val allSeasons = if (entryPageConsumed) {
            seasons
        } else {
            val (cards, seasonChanged) = harvestSeasonEpisodes(language, pageTitleId, btf)
            changed = changed || seasonChanged
            val fallbackNumber = header.seasonNumber ?: 1
            listOf(PvSeasonData(fallbackNumber, cards)) + seasons
        }

        return baseShow.copy(seasons = allSeasons) to changed
    }

    /**
     * Harvests every episode card of one season page, inline cards first then paginated ones.
     *
     * @param language The page language of the country.
     * @param pageTitleId The GTI of the season page, sent as `titleID` of the widget requests.
     * @param btf The state of the season page.
     * @return The harvested [PvEpisodeCard] list and whether any fetched payload changed.
     * @see [PvEpisodeCard]
     * @see [fetchWidgets]
     */
    private suspend fun harvestSeasonEpisodes(
        language: String,
        pageTitleId: String,
        btf: PrimeVideoState
    ): Pair<List<PvEpisodeCard>, Boolean> {
        val cards = LinkedHashMap<String, PvEpisodeCard>()
        var changed = false

        extractInlineEpisodes(language, btf).forEach { cards[it.gti] = it }

        var token = nextPageToken(btf.episodeList.actions)
        var pages = 0
        while (token != null && pages < MAX_WIDGET_PAGES) {
            val episodeList = try {
                val response = fetchWidgets(language, pageTitleId, pages, token)
                changed = changed || response.hasChanged
                response.data.widgets?.episodeList
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A challenged widget request must not discard the inline cards already harvested.
                logger.warning(
                    "Prime Video widget pagination stopped for $pageTitleId ($language): ${e.message}"
                )
                break
            } ?: break
            extractWidgetEpisodes(language, episodeList).forEach { cards[it.gti] = it }
            token = nextPageToken(episodeList.actions)
            pages++
        }

        return cards.values.toList() to changed
    }

    /**
     * Extracts the episode cards embedded in the `detail`, `self` and `metadata` maps of a
     * season page.
     *
     * @param language The page language of the country.
     * @param btf The state of the season page.
     * @return The harvested [PvEpisodeCard] list of the page.
     * @see [PvEpisodeCard]
     */
    private fun extractInlineEpisodes(
        language: String,
        btf: PrimeVideoState
    ): List<PvEpisodeCard> =
        btf.detail.episodes.mapNotNull { (gti, entity) ->
            if (entity.titleType != "episode")
                return@mapNotNull null
            entity.toEpisodeCard(
                language = language,
                gti = gti,
                self = btf.self[gti],
                metadata = btf.metadata[gti]
            )
        }

    /**
     * Extracts the episode cards of a `getDetailWidgets` episode list payload.
     *
     * @param language The page language of the country.
     * @param episodeList The `episodeList` object of the widget response.
     * @return The harvested [PvEpisodeCard] list of the widget page.
     * @see [PvEpisodeCard]
     */
    private fun extractWidgetEpisodes(
        language: String,
        episodeList: PrimeVideoWidgetEpisodeList
    ): List<PvEpisodeCard> =
        episodeList.episodes.mapNotNull { item ->
            val detail = item.detail ?: return@mapNotNull null
            if (detail.titleType != "episode")
                return@mapNotNull null
            val gti = detail.catalogId ?: item.titleID
                ?: return@mapNotNull null
            detail.toEpisodeCard(
                language = language,
                gti = gti,
                self = item.self,
                metadata = item.metadata
            )
        }

    /**
     * Converts a harvested show into [PlatformEpisode] objects for a specific country.
     *
     * @param show The harvested [PvShowData] of the show.
     * @param country Target country distribution.
     * @param language The page language the show was harvested in.
     * @return The normalized [PlatformEpisode] objects of the show.
     * @see [PlatformEpisode]
     * @see [PvShowData.toPlatformAnime]
     */
    private fun convertShow(
        show: PvShowData,
        country: Country,
        language: String
    ): List<PlatformEpisode> {
        val anime = show.toPlatformAnime(country) ?: return emptyList()

        return show.seasons.flatMap { season ->
            season.episodes.mapNotNull { card ->
                try {
                    card.toPlatformEpisode(anime, show, season.number, country, language)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: MissingAudioLocaleException) {
                    logger.warning(e.message)
                    null
                } catch (e: Exception) {
                    logger.warning(
                        "Skipping Prime Video episode ${card.gti}: ${e.message}"
                    )
                    null
                }
            }
        }
    }

    /**
     * Fetches one Prime Video detail page as a JSON payload.
     *
     * The `dvWebAppClientVersion` query parameter and the `WebAppSPA` marker negotiate the JSON
     * representation of the page instead of the HTML document.
     *
     * @param language The page language segment of the detail URL (`fr`, `de`, `pl`...).
     * @param compactId The compact GTI of the detail page.
     * @return The [SmartResponse] carrying the parsed page payload.
     * @see [SmartHttpClient]
     */
    private suspend fun fetchPage(
        language: String,
        compactId: String
    ): SmartResponse<PrimeVideoPageResponse> =
        client.get(
            url = "$BASE_URL/-/$language/detail/$compactId" +
                "?dvWebAppClientVersion=$CLIENT_VERSION",
            cacheKey = "prime_video:detail:$language:$compactId",
            ttl = ttl,
            headers = pageHeaders(language)
        )

    /**
     * Fetches one page of episode cards through the `getDetailWidgets` endpoint.
     *
     * The request goes through [widgetClient], whose Ktor client never follows redirects:
     * the endpoint's bot check answers the shared client with a redirect loop, while the
     * redirect-free client negotiates the JSON payload. The cache key carries the page index
     * rather than the rotating token so the stale fallback can serve the last good page.
     *
     * @param language The page language of the country.
     * @param pageTitleId The GTI of the season page, sent as `titleID`.
     * @param pageIndex The index of the widget page within the season pagination.
     * @param token The `NextPage` widget token of the previous page.
     * @return The [SmartResponse] carrying the widget payload.
     * @see [SmartHttpClient]
     */
    private suspend fun fetchWidgets(
        language: String,
        pageTitleId: String,
        pageIndex: Int,
        token: String
    ): SmartResponse<PrimeVideoWidgetsResponse> {
        val widgets = "[{\"widgetType\":\"EpisodeList\",\"widgetToken\":\"$token\"}]"
        return widgetClient.get(
            url = "$BASE_URL/api/getDetailWidgets?titleID=$pageTitleId" +
                "&widgets=${URLEncoder.encode(widgets, StandardCharsets.UTF_8)}",
            cacheKey = "prime_video:widgets:$language:$pageTitleId:$pageIndex",
            ttl = ttl,
            headers = pageHeaders(language)
        )
    }

    /**
     * Builds the headers of the Prime Video web endpoints.
     *
     * @param language The page language sent as `Accept-Language`.
     * @return The request headers of the platform.
     */
    private fun pageHeaders(language: String): Map<String, String> = mapOf(
        HttpHeaders.Accept to ContentType.Application.Json.toString(),
        HttpHeaders.AcceptLanguage to language,
        HttpHeaders.UserAgent to USER_AGENT,
        "x-requested-with" to "WebAppSPA"
    )

    /**
     * Resolves the page language segment of a country distribution.
     *
     * @param country Target country distribution.
     * @return The language prefix of the country default locale, lowercased.
     * @see [Country]
     */
    private fun pageLanguage(country: Country): String =
        country.defaultLocale.code.substringBefore('-').lowercase()

    /**
     * Resolves the language of a country distribution for the dubbed/subtitled filter.
     *
     * @param country Target country distribution.
     * @return The language prefix of the country default locale, lowercased.
     * @see [Country]
     */
    private fun countryLanguage(country: Country): String = pageLanguage(country)

    /**
     * Finds the `NextPage` widget token of an episode list actions block.
     *
     * @param actions The `actions` object carrying the pagination entries.
     * @return The next page token, or `null` when the list is exhausted.
     */
    private fun nextPageToken(actions: PrimeVideoEpisodeActions?): String? =
        actions?.pagination
            ?.firstOrNull { it.tokenType == "NextPage" }
            ?.token

    /**
     * Parses a localized Prime Video release date into a UTC [ZonedDateTime].
     *
     * The date is rendered in the page language (`28 mai 2018`, `May 28, 2018`...), so the
     * formatters of that language are tried first, then the formatters of the other supported
     * languages as a fallback.
     *
     * @param raw The raw release date string of the platform.
     * @param language The page language the date was rendered in.
     * @return The parsed date at midnight UTC, or `null` when it cannot be parsed.
     */
    private fun parseReleaseDate(raw: String?, language: String): ZonedDateTime? {
        val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return releaseDateFormatters(language).firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }
                .getOrNull()
                ?.atStartOfDay(ZoneOffset.UTC)
        }
    }

    /**
     * Returns the release date formatters of a page language, completed by the fallback ones.
     *
     * @param language The page language the dates are rendered in.
     * @return The [DateTimeFormatter] list to try, in order.
     */
    private fun releaseDateFormatters(language: String): List<DateTimeFormatter> =
        releaseDateFormatters.getOrPut(language) {
            val locale = when (language) {
                "fr" -> java.util.Locale.FRENCH
                "de" -> java.util.Locale.GERMAN
                "pl" -> java.util.Locale.forLanguageTag("pl")
                "ca" -> java.util.Locale.forLanguageTag("ca")
                else -> java.util.Locale.ENGLISH
            }
            val patterns = when (language) {
                "fr" -> listOf("d MMMM uuuu", "d MMM uuuu")
                "de" -> listOf("d. MMMM uuuu", "d. MMM uuuu", "d MMMM uuuu")
                "pl" -> listOf("d MMMM uuuu", "d MMM uuuu", "dd.MM.yyyy", "d.MM.yyyy")
                else -> listOf("MMMM d, uuuu", "MMM d, uuuu", "d MMMM uuuu", "d MMM uuuu")
            }
            patterns.map { DateTimeFormatter.ofPattern(it, locale) } +
                FALLBACK_DATE_FORMATTERS
        }

    /**
     * Maps a Prime Video audio or subtitle track name to its ISO 639-1 language code.
     *
     * Track names are rendered in the page language but some languages keep their native name
     * (`日本語` on a French page); both localized and native labels are recognized. Decorations
     * such as `[Audiodescription]`, `[CC]`, `[auto]` and `Dialogue Boost` qualifiers are ignored.
     *
     * @param name The raw track name of the platform.
     * @return The ISO 639-1 language code, or `null` when the name is unknown.
     */
    private fun trackLanguage(name: String): String? =
        TRACK_LANGUAGE_CODES[normalizeTrackName(name)]

    /**
     * Resolves the canonical [Locale] of an audio track language for a country distribution.
     *
     * The French audio of a Francophone distribution resolves to the default locale of the
     * country (so `fr-BE` keeps its own locale); the other languages resolve to their canonical
     * locale of the [Locale] enum, and unmapped languages return `null`.
     *
     * @param language The ISO 639-1 language code of the track.
     * @param country Target country distribution.
     * @return The canonical [Locale], or `null` when the language is not distributed.
     * @see [Locale]
     * @see [Country]
     */
    private fun audioLocale(language: String, country: Country): Locale? =
        when (language) {
            "fr" -> if (country.defaultLocale.code.startsWith("fr")) {
                country.defaultLocale
            } else {
                Locale.FR_FR
            }
            "ja" -> Locale.JA_JP
            "en" -> Locale.EN_US
            "de" -> Locale.DE_DE
            "pl" -> Locale.PL_PL
            "zh" -> Locale.ZH_CN
            "ko" -> Locale.KO_KR
            else -> null
        }

    /**
     * Converts a harvested episode card into a platform episode representation.
     *
     * Applies the release filters of the platform: upcoming and unlaunched cards are rejected,
     * releases are kept only when they are dubbed or subtitled in the language of the target
     * [country], and every resolvable audio track becomes an audio [Locale] of the release.
     *
     * @param anime The parent [PlatformAnime] instance resolved for this episode.
     * @param show The harvested [PvShowData] the episode belongs to.
     * @param seasonNumber The number of the season the card was harvested from.
     * @param country Target country distribution.
     * @param language The page language the show was harvested in.
     * @return The normalized [PlatformEpisode], or `null` when the release is filtered out.
     * @throws MissingAudioLocaleException If the release carries no resolvable audio locale.
     * @see [PlatformEpisode]
     * @see [Locale]
     */
    private fun PvEpisodeCard.toPlatformEpisode(
        anime: PlatformAnime,
        show: PvShowData,
        seasonNumber: Int,
        country: Country,
        language: String
    ): PlatformEpisode? {
        if (isUpcoming || !isLaunched)
            return null

        val releaseDateTime = parseReleaseDate(releaseDate, language) ?: return null
        val episodeUrl = url ?: return null

        val audioLanguages = audioTracks.mapNotNull(::trackLanguage)
        val audioLocales = audioLanguages.mapNotNull { audioLocale(it, country) }.distinct()
        val targetLanguage = countryLanguage(country)
        val hasCountryAudio = audioLanguages.contains(targetLanguage)
        val hasCountrySubtitle = subtitles.mapNotNull(::trackLanguage).contains(targetLanguage)

        if (!hasCountryAudio && !hasCountrySubtitle)
            return null

        if (audioLocales.isEmpty())
            throw MissingAudioLocaleException(
                episodeId = gti,
                episodeName = title,
                languages = audioTracks,
                country = country
            )

        val number = episodeNumber ?: if (show.isMovie) 1 else -1
        val episodeType = when {
            show.isMovie -> EpisodeType.FILM
            number < 0 -> EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        return PlatformEpisode(
            anime = anime,
            id = compactGti ?: gti,
            image = imageUrl ?: show.bannerImage.orEmpty(),
            releaseDateTime = releaseDateTime,
            season = if (show.isMovie) 1 else seasonNumber,
            number = number,
            episodeType = episodeType,
            duration = durationSeconds ?: 0L,
            url = episodeUrl,
            audioLocales = audioLocales,
            uncensored = false,
            localizations = listOf(
                PlatformEpisodeLocalization(
                    country = country,
                    title = title.sanitizeText(),
                    description = synopsis.sanitizeText()
                )
            )
        )
    }

    /**
     * Converts a harvested show into the platform anime domain entity.
     *
     * @param country Target country distribution, used as the key of the produced localization.
     * @return The normalized [PlatformAnime], or `null` when the show carries no usable title.
     * @see [PlatformAnime]
     */
    private fun PvShowData.toPlatformAnime(country: Country): PlatformAnime? {
        val animeTitle = (parentTitle ?: title)?.sanitizeText() ?: return null

        return PlatformAnime(
            id = compactId,
            platform = Platform.PRIME_VIDEO,
            images = listOfNotNull(
                bannerImage?.let { PlatformImage(ImageType.BANNER, it) },
                thumbnailImage?.let { PlatformImage(ImageType.THUMBNAIL, it) },
                logoImage?.let { PlatformImage(ImageType.TITLE, it) }
            ),
            localizations = listOf(
                PlatformAnimeLocalization(
                    country = country,
                    locale = country.defaultLocale,
                    title = animeTitle,
                    description = synopsis.sanitizeText()
                )
            )
        )
    }

    private companion object {
        /**
         * Release date formatters shared by every page language, tried after the language ones.
         */
        val FALLBACK_DATE_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMMM d, uuuu", java.util.Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", java.util.Locale.FRENCH),
            DateTimeFormatter.ofPattern("d MMM uuuu", java.util.Locale.FRENCH)
        )

        /**
         * Track name to ISO 639-1 language code, covering the localized and native labels
         * Prime Video publishes for audio tracks and subtitles.
         */
        val TRACK_LANGUAGE_CODES = mapOf(
            "francais" to "fr", "francaise" to "fr",
            "french" to "fr", "franzosisch" to "fr", "francuski" to "fr",
            "japonais" to "ja", "japonaise" to "ja",
            "japanese" to "ja", "japanisch" to "ja", "japonski" to "ja", "日本語" to "ja",
            "anglais" to "en", "anglaise" to "en",
            "english" to "en", "englisch" to "en", "angielski" to "en",
            "allemand" to "de", "allemande" to "de",
            "german" to "de", "deutsch" to "de", "niemiecki" to "de",
            "espagnol" to "es", "espagnole" to "es",
            "spanish" to "es", "spanisch" to "es", "hiszpanski" to "es",
            "italien" to "it", "italienne" to "it",
            "italian" to "it", "italienisch" to "it", "wloski" to "it",
            "portugais" to "pt", "portugaise" to "pt",
            "portuguese" to "pt", "portugiesisch" to "pt", "portugalski" to "pt",
            "polonais" to "pl", "polonaise" to "pl",
            "polish" to "pl", "polnisch" to "pl",
            "coreen" to "ko", "coreenne" to "ko",
            "korean" to "ko", "koreanisch" to "ko", "koreanski" to "ko", "한국어" to "ko",
            "chinois" to "zh", "chinoise" to "zh",
            "chinese" to "zh", "chinesisch" to "zh", "chinski" to "zh", "中文" to "zh",
            "neerlandais" to "nl", "neerlandaise" to "nl",
            "dutch" to "nl", "niederlandisch" to "nl", "holenderski" to "nl",
            "russe" to "ru", "russian" to "ru", "russisch" to "ru",
            "rosyjski" to "ru", "русский" to "ru",
            "suedois" to "sv", "suedoise" to "sv",
            "swedish" to "sv", "schwedisch" to "sv", "szwedzki" to "sv", "svenska" to "sv",
            "danois" to "da", "danish" to "da", "danisch" to "da",
            "dunski" to "da", "dansk" to "da",
            "norvegien" to "no", "norwegian" to "no", "norwegisch" to "no",
            "norweski" to "no", "norsk" to "no",
            "finnois" to "fi", "finnish" to "fi", "finnisch" to "fi",
            "finski" to "fi", "suomi" to "fi",
            "tcheque" to "cs", "czech" to "cs", "tschechisch" to "cs",
            "czeski" to "cs", "cestina" to "cs",
            "hongrois" to "hu", "hungarian" to "hu", "ungarisch" to "hu",
            "wegierski" to "hu", "magyar" to "hu",
            "roumain" to "ro", "roumaine" to "ro", "romanian" to "ro",
            "rumannisch" to "ro", "rumunski" to "ro", "romana" to "ro",
            "turc" to "tr", "turque" to "tr", "turkish" to "tr",
            "turkisch" to "tr", "turecki" to "tr", "turkce" to "tr",
            "thailandais" to "th", "thai" to "th", "thailandisch" to "th",
            "tajski" to "th", "ไทย" to "th",
            "indonesien" to "id", "indonesian" to "id", "indonesisch" to "id",
            "indonezyjski" to "id", "bahasa indonesia" to "id",
            "malais" to "ms", "malay" to "ms", "malaiisch" to "ms", "malajski" to "ms",
            "hindi" to "hi", "हिन्दी" to "hi", "हिंदी" to "hi",
            "arabe" to "ar", "arabic" to "ar", "arabisch" to "ar",
            "arabski" to "ar", "العربية" to "ar",
            "hebreu" to "he", "hebrew" to "he", "hebraisch" to "he",
            "hebrajski" to "he", "עברית" to "he",
            "ukrainien" to "uk", "ukrainian" to "uk", "ukrainisch" to "uk",
            "ukrainski" to "uk", "українська" to "uk",
            "grec" to "el", "greek" to "el", "griechisch" to "el",
            "grecki" to "el", "ελληνικά" to "el",
            "vietnamien" to "vi", "vietnamese" to "vi", "vietnamesisch" to "vi",
            "wietnamski" to "vi", "tieng viet" to "vi"
        )
    }
}

/**
 * Raw catalog data of one tracked Prime Video show.
 *
 * @param compactId The compact GTI of the show, published as the platform identifier.
 * @param title The title of the show as rendered on the entry page.
 * @param parentTitle The parent title of the show, preferred over [title] when present.
 * @param synopsis The synopsis of the show.
 * @param bannerImage The landscape key art of the show.
 * @param thumbnailImage The portrait poster of the show.
 * @param logoImage The transparent title logo of the show.
 * @param isMovie Whether the show is a movie rather than a series.
 * @param seasons The harvested seasons of the show.
 */
private data class PvShowData(
    val compactId: String,
    val title: String?,
    val parentTitle: String?,
    val synopsis: String?,
    val bannerImage: String?,
    val thumbnailImage: String?,
    val logoImage: String?,
    val isMovie: Boolean,
    val seasons: List<PvSeasonData> = emptyList()
)

/**
 * Raw catalog data of one season of a tracked Prime Video show.
 *
 * @param number The sequence number of the season.
 * @param episodes The harvested episode cards of the season.
 */
private data class PvSeasonData(
    val number: Int,
    val episodes: List<PvEpisodeCard>
)

/**
 * Raw catalog data of one Prime Video episode card.
 *
 * @param gti The GTI of the episode entity.
 * @param compactGti The compact GTI of the episode, published as the platform identifier.
 * @param url The watch URL of the episode.
 * @param title The title of the episode.
 * @param synopsis The synopsis of the episode.
 * @param episodeNumber The episode number within its season, `null` when unnumbered.
 * @param durationSeconds The runtime of the episode in seconds.
 * @param releaseDate The localized release date of the episode.
 * @param audioTracks The raw audio track names of the episode.
 * @param subtitles The raw subtitle track names of the episode.
 * @param imageUrl The landscape image of the episode.
 * @param isUpcoming Whether the card announces an episode that is not out yet.
 * @param isLaunched Whether the episode is already available.
 */
private data class PvEpisodeCard(
    val gti: String,
    val compactGti: String?,
    val url: String?,
    val title: String?,
    val synopsis: String?,
    val episodeNumber: Int?,
    val durationSeconds: Long?,
    val releaseDate: String?,
    val audioTracks: List<String>,
    val subtitles: List<String>,
    val imageUrl: String?,
    val isUpcoming: Boolean,
    val isLaunched: Boolean
)

@Serializable
internal data class PrimeVideoPageResponse(
    val body: PrimeVideoPageBody = PrimeVideoPageBody()
)

@Serializable
internal data class PrimeVideoPageBody(
    val atf: PrimeVideoStateWrapper = PrimeVideoStateWrapper(),
    val btf: PrimeVideoStateWrapper = PrimeVideoStateWrapper()
)

@Serializable
internal data class PrimeVideoStateWrapper(
    val state: PrimeVideoState? = null
)

@Serializable
internal data class PrimeVideoState(
    val pageTitleId: String? = null,
    val detail: PrimeVideoDetailState = PrimeVideoDetailState(),
    val seasons: Map<String, List<PrimeVideoSeason>> = emptyMap(),
    val self: Map<String, PrimeVideoEpisodeSelf> = emptyMap(),
    val metadata: Map<String, PrimeVideoEpisodeMetadata> = emptyMap(),
    val episodeList: PrimeVideoEpisodeList = PrimeVideoEpisodeList()
)

@Serializable
internal data class PrimeVideoDetailState(
    val headerDetail: Map<String, PrimeVideoHeader> = emptyMap(),
    @SerialName("detail")
    val episodes: Map<String, PrimeVideoEpisodeEntity> = emptyMap()
)

@Serializable
internal data class PrimeVideoHeader(
    val title: String? = null,
    val parentTitle: String? = null,
    val synopsis: String? = null,
    val entityType: String? = null,
    val titleType: String? = null,
    val seasonNumber: Int? = null,
    val duration: Long? = null,
    val releaseDate: String? = null,
    val audioTracks: List<String> = emptyList(),
    val subtitles: List<String> = emptyList(),
    val images: PrimeVideoImages? = null
)

@Serializable
internal data class PrimeVideoEpisodeEntity(
    val catalogId: String? = null,
    val title: String? = null,
    val synopsis: String? = null,
    val entityType: String? = null,
    val titleType: String? = null,
    val episodeNumber: Int? = null,
    val duration: Long? = null,
    val releaseDate: String? = null,
    val audioTracks: List<String> = emptyList(),
    val subtitles: List<String> = emptyList(),
    val images: PrimeVideoImages? = null
)

@Serializable
internal data class PrimeVideoImages(
    val covershot: String? = null,
    val heroshot: String? = null,
    val packshot: String? = null,
    val titleshot: String? = null,
    val titleLogo: String? = null
)

@Serializable
internal data class PrimeVideoEpisodeSelf(
    val compactGTI: String? = null,
    val gti: String? = null,
    val link: String? = null,
    val sequenceNumber: Int? = null,
    val titleType: String? = null,
    val isLaunched: Boolean? = null
)

@Serializable
internal data class PrimeVideoEpisodeMetadata(
    val traits: List<String> = emptyList(),
    val upcomingSynopsis: String? = null
)

@Serializable
internal data class PrimeVideoSeason(
    val seasonId: String? = null,
    val seasonLink: String? = null,
    val sequenceNumber: Int? = null,
    val isSelected: Boolean? = null
)

@Serializable
internal data class PrimeVideoEpisodeList(
    val actions: PrimeVideoEpisodeActions? = null
)

@Serializable
internal data class PrimeVideoEpisodeActions(
    val pagination: List<PrimeVideoPageAction> = emptyList()
)

@Serializable
internal data class PrimeVideoPageAction(
    val token: String? = null,
    val tokenType: String? = null
)

@Serializable
internal data class PrimeVideoWidgetsResponse(
    val widgets: PrimeVideoWidgets? = null
)

@Serializable
internal data class PrimeVideoWidgets(
    val episodeList: PrimeVideoWidgetEpisodeList? = null
)

@Serializable
internal data class PrimeVideoWidgetEpisodeList(
    val episodes: List<PrimeVideoWidgetEpisode> = emptyList(),
    val actions: PrimeVideoEpisodeActions? = null
)

@Serializable
internal data class PrimeVideoWidgetEpisode(
    val detail: PrimeVideoEpisodeEntity? = null,
    val self: PrimeVideoEpisodeSelf? = null,
    val metadata: PrimeVideoEpisodeMetadata? = null,
    val titleID: String? = null
)

/**
 * Converts a header entity into an episode card, used for movies harvested as a single film.
 *
 * @param language The page language of the country.
 * @param gti The GTI published as the episode identifier fallback.
 * @param compactGti The compact GTI published as the episode identifier.
 * @param fallbackUrl The watch URL used when no link is available.
 * @return The harvested [PvEpisodeCard].
 */
private fun PrimeVideoHeader.toEpisodeCard(
    language: String,
    gti: String,
    compactGti: String?,
    fallbackUrl: String
): PvEpisodeCard =
    PvEpisodeCard(
        gti = gti,
        compactGti = compactGti,
        url = fallbackUrl,
        title = title,
        synopsis = synopsis,
        episodeNumber = null,
        durationSeconds = duration,
        releaseDate = releaseDate,
        audioTracks = audioTracks,
        subtitles = subtitles,
        imageUrl = images.bannerImageUrl(),
        isUpcoming = false,
        isLaunched = true
    )

/**
 * Converts an episode entity into an episode card.
 *
 * @param language The page language of the country.
 * @param gti The GTI of the episode entity.
 * @param self The `self` entry carrying the watch link and launch state.
 * @param metadata The `metadata` entry carrying the release traits.
 * @param fallbackUrl The watch URL used when neither link nor compact GTI is available.
 * @return The harvested [PvEpisodeCard].
 */
private fun PrimeVideoEpisodeEntity.toEpisodeCard(
    language: String,
    gti: String,
    self: PrimeVideoEpisodeSelf?,
    metadata: PrimeVideoEpisodeMetadata?,
    fallbackUrl: String? = null
): PvEpisodeCard {
    val compactGti = self?.compactGTI
    val link = self?.link
    val url = link?.let { "$BASE_URL${it.substringBefore('?')}?autoplay=1&t=0" }
        ?: compactGti?.let { "$BASE_URL/-/$language/detail/$it?autoplay=1&t=0" }
        ?: fallbackUrl

    return PvEpisodeCard(
        gti = gti,
        compactGti = compactGti,
        url = url,
        title = title,
        synopsis = synopsis,
        episodeNumber = episodeNumber ?: self?.sequenceNumber,
        durationSeconds = duration,
        releaseDate = releaseDate,
        audioTracks = audioTracks,
        subtitles = subtitles,
        imageUrl = images.bannerImageUrl(),
        isUpcoming = metadata?.traits?.isNotEmpty() == true &&
            !metadata.upcomingSynopsis.isNullOrBlank(),
        isLaunched = self?.isLaunched != false
    )
}

/**
 * Selects the landscape image of an episode card and normalizes it to the banner dimensions.
 *
 * Covershots are 16:9 rasters resized directly; packshots and titleshots are 4:3 rasters
 * cropped to 16:9 first so the result is not stretched.
 *
 * @return The normalized image URL, or `null` when the card carries no usable image.
 */
private fun PrimeVideoImages?.bannerImageUrl(): String? {
    this ?: return null
    covershot?.takeIf(String::isNotBlank)?.let { return cdnImage(it, CDN_BANNER) }
    val fallback = packshot?.takeIf(String::isNotBlank)
        ?: titleshot?.takeIf(String::isNotBlank)
    return fallback?.let { cdnImage(it, CDN_BANNER_FROM_4X3) }
}

/**
 * Rewrites a raw Amazon image URL into canonical dimensions through CDN operations.
 *
 * Prime Video detail payloads carry unresized `pv-target-images` assets whose aspect
 * ratio varies by role. Amazon's image service applies chained `._<op>_` operations
 * before the file extension, so the [operations] string is inserted there. URLs that
 * are not served by the Amazon image CDN, or that already carry operations, are
 * returned unchanged.
 *
 * @param url The raw image URL found in a Prime Video payload.
 * @param operations The CDN operations to append, joined by `_` without surrounding
 * underscores (e.g. `UR1920,1080`).
 * @return The normalized image URL, or `null` when [url] is null or blank.
 */
private fun cdnImage(url: String?, operations: String): String? {
    val trimmed = url?.takeIf(String::isNotBlank) ?: return null
    if (!trimmed.contains("amazon.com/images/")) return trimmed
    val extensionDot = trimmed.lastIndexOf('.')
    if (extensionDot <= 0) return trimmed
    val base = trimmed.substring(0, extensionDot)
    if (base.contains("._")) return trimmed
    return "$base._${operations}_.${trimmed.substring(extensionDot + 1)}"
}

/**
 * Normalizes a Prime Video track name for the language lookup.
 *
 * @return the lowercase, accent-free base name of the track.
 */
private fun normalizeTrackName(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .substringBefore('[')
        .substringBefore("dialogue boost")
        .substringBefore("audio description")
        .substringBefore("descriptive audio")
        .trim()
