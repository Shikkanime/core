package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.SmartResponse
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformAnimeLocalization
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.PlatformEpisodeLocalization
import fr.shikkanime.jobs.platforms.PlatformImage
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.jobs.platforms.exceptions.MissingAudioLocaleException
import fr.shikkanime.jobs.platforms.exceptions.TeaserEpisodeException
import fr.shikkanime.jobs.platforms.sanitizeText
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import java.time.ZonedDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

private val SEASON_REGEX = Regex(""" Saison (\d+)""")
private val TEASER_REGEX = Regex("""\b(?:teaser|pv|trailer)\b""", RegexOption.IGNORE_CASE)
private val SPECIAL_EPISODE_REGEX = Regex("""^SP(\d+)$""")
private val IDENTIFIER_REGEX = Regex("""^[^|]+\|([^|]+)\|[^|]+$""")
private val SERIES_ID_REGEX = Regex("""^[A-Za-z0-9]+$""")

/**
 * Returns the first candidate that is neither `null` nor blank.
 *
 * @param candidates the raw values to pick from, in priority order.
 * @return the first non-blank candidate, or `null` when every candidate is blank.
 */
private fun firstNonBlank(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }

private const val BASE_URL = "https://www.crunchyroll.com"

/**
 * Streaming platform connector for Crunchyroll.
 *
 * This implementation connects to the Crunchyroll public content API: it mints an anonymous
 * access token with the Android TV basic auth credentials, browses the latest episode releases
 * per configured country, resolves the parent series objects for their artwork and editorial
 * metadata, and normalizes everything into [PlatformEpisode] instances. The series objects of
 * a browse page are resolved in a single batched request so a harvest costs one browse call and
 * one objects call per country.
 *
 * Only the latest published episodes are harvested: the upcoming episode prediction of the
 * legacy implementation is intentionally not ported.
 *
 * @param client The [SmartHttpClient] instance used to execute every cached HTTP request of
 * the platform, including the token endpoint POST.
 * @param ttl The cache time-to-live [Duration] for browse and series responses (defaults to 1 minute).
 * @param countries The target countries enabled for this platform connector.
 * @see [StreamingPlatform]
 * @see [PlatformEpisode]
 * @see [PlatformAnime]
 * @see [Platform.CRUNCHYROLL]
 */
@Single(binds = [StreamingPlatform::class])
class CrunchyrollPlatform(
    private val client: SmartHttpClient,
    private val ttl: Duration = 1.minutes,
    override val countries: Set<Country> = setOf(Country.FR)
) : StreamingPlatform {
    private data class CachedAccessToken(
        val token: String,
        val expiresAtMillis: Long
    )

    override val platform: Platform = Platform.CRUNCHYROLL

    private val logger = LoggerFactory.getLogger()
    private var cachedEpisodes: List<PlatformEpisode> = emptyList()
    private val cachedEpisodesByCountry = mutableMapOf<Country, List<PlatformEpisode>>()
    private val deviceId = Uuid.random().toString()
    private var cachedAccessToken: CachedAccessToken? = null
    private val accessTokenMutex = Mutex()

    /**
     * Retrieves the latest episodes published on Crunchyroll across configured countries.
     *
     * For every country declared in [countries], queries the discover browse endpoint sorted by
     * newly added episodes, resolves the series of every candidate and maps valid releases into
     * [PlatformEpisode] instances.
     *
     * In-memory cache is returned while [SmartHttpClient] reports that the remote payloads have
     * not changed. A Crunchyroll identifier is unique to an audio locale, so episodes harvested
     * for several countries are kept as distinct entries.
     *
     * @return A [SmartResponse] carrying the normalized [PlatformEpisode] objects and the change
     * flag of the Crunchyroll payloads.
     * @see [SmartResponse]
     * @see [PlatformEpisode]
     * @see [CrunchyrollBrowseObject.toPlatformEpisode]
     */
    override suspend fun fetchLatestEpisodes(): SmartResponse<List<PlatformEpisode>> {
        if (countries.isEmpty())
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
     * Fetches and maps the latest Crunchyroll episode releases for a specific country.
     *
     * @param country Target country distribution.
     * @return The mapped [PlatformEpisode] list of the country and whether the payload changed.
     * @see [Country]
     * @see [PlatformEpisode]
     */
    private suspend fun fetchEpisodesForCountry(
        country: Country
    ): Pair<List<PlatformEpisode>, Boolean> {
        val locale = country.defaultLocale.code
        val response = client.get<CrunchyrollBrowseResponse>(
            url = "$BASE_URL/content/v2/discover/browse" +
                "?sort_by=newly_added&type=episode&n=$BROWSE_PAGE_SIZE&start=0&locale=$locale",
            cacheKey = "crunchyroll:browse:$locale",
            ttl = ttl,
            headers = authorizationHeaders()
        )

        val hasChanged = response.hasChanged || country !in cachedEpisodesByCountry

        if (!hasChanged)
            return cachedEpisodesByCountry[country].orEmpty() to false

        val episodes = convertBrowseObjects(response.data.data, country)
        cachedEpisodesByCountry[country] = episodes
        return episodes to true
    }

    /**
     * Converts a page of Crunchyroll browse objects into domain [PlatformEpisode] objects.
     *
     * Resolves the series objects of every episode through batched requests, caches the produced
     * [PlatformAnime] per series so episodes of a same show share the same instance, and drops
     * teasers, releases without audio or subtitles for the target country.
     *
     * @param browseObjects The raw browse objects of the discover endpoint.
     * @param country Target country distribution.
     * @return The normalized [PlatformEpisode] objects of the page.
     * @see [PlatformEpisode]
     * @see [CrunchyrollBrowseObject.toPlatformEpisode]
     */
    private suspend fun convertBrowseObjects(
        browseObjects: List<CrunchyrollBrowseObject>,
        country: Country
    ): List<PlatformEpisode> {
        val episodeEntries = browseObjects.mapNotNull { browseObject ->
            if (browseObject.id.isBlank())
                return@mapNotNull null
            browseObject.episodeMetadata?.let { metadata -> browseObject to metadata }
        }

        if (episodeEntries.isEmpty())
            return emptyList()

        val seriesById = fetchSeriesObjects(
            country,
            episodeEntries.mapNotNull { it.second.seriesId?.takeIf(String::isNotBlank) }.toSet()
        )

        val animes = mutableMapOf<String, PlatformAnime>()

        return episodeEntries.mapNotNull { (browseObject, metadata) ->
            val seriesId = metadata.seriesId?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val series = seriesById[seriesId]
            if (series == null) {
                logger.warning(
                    "Skipping Crunchyroll episode ${browseObject.id}: series $seriesId not found"
                )
                return@mapNotNull null
            }

            val anime = animes.getOrPut(seriesId) {
                series.toPlatformAnime(seriesId, metadata.seriesTitle, country)
            }

            var conversionFailed = false
            val episode = try {
                browseObject.toPlatformEpisode(metadata, anime, series, country)
            } catch (e: TeaserEpisodeException) {
                logger.fine(e.message)
                conversionFailed = true
                null
            } catch (e: MissingAudioLocaleException) {
                logger.warning(e.message)
                conversionFailed = true
                null
            } catch (e: Exception) {
                logger.warning(
                    "Skipping Crunchyroll episode ${browseObject.id}: ${e.message}"
                )
                conversionFailed = true
                null
            }

            if (episode == null && !conversionFailed)
                logger.fine("Crunchyroll episode ${browseObject.id} filtered out")

            episode
        }
    }

    /**
     * Fetches the series browse objects of the given identifiers for a country.
     *
     * Every identifier is requested in a single batched call, like the legacy implementation
     * did with its chunks: the browse page only carries a handful of series, so one request
     * resolves them all. The cache key is built from the sorted identifiers, so it stays stable
     * as long as the catalog does not change.
     *
     * @param country Target country distribution.
     * @param seriesIds The series identifiers to resolve.
     * @return The fetched browse objects indexed by their identifier.
     * @see [Country]
     * @see [CrunchyrollBrowseObject]
     */
    private suspend fun fetchSeriesObjects(
        country: Country,
        seriesIds: Set<String>
    ): Map<String, CrunchyrollBrowseObject> {
        if (seriesIds.isEmpty())
            return emptyMap()

        val locale = country.defaultLocale.code
        val ids = seriesIds.filter { SERIES_ID_REGEX.matches(it) }.sorted().joinToString(",")
        if (ids.isEmpty())
            return emptyMap()

        return client.get<CrunchyrollBrowseResponse>(
            url = "$BASE_URL/content/v2/cms/objects/$ids?images=true&locale=$locale",
            cacheKey = "crunchyroll:objects:$locale:$ids",
            ttl = ttl,
            headers = authorizationHeaders()
        ).data.data.associateBy { it.id }
    }

    /**
     * Builds the bearer authorization headers of the Crunchyroll content API.
     *
     * @return The authorization headers carrying the current anonymous access token.
     * @see [accessToken]
     */
    private suspend fun authorizationHeaders(): Map<String, String> =
        mapOf(HttpHeaders.Authorization to "Bearer ${accessToken()}")

    /**
     * Returns a valid anonymous access token for the Crunchyroll content API.
     *
     * The token is minted once per [CrunchyrollPlatform] instance and reused until shortly
     * before its expiry: the [deviceId] identifies this client across the whole lifetime of
     * the token, and the token endpoint is only queried again once the cached token expires.
     * The POST itself goes through [SmartHttpClient], which additionally serves a stale token
     * when the endpoint is failing.
     *
     * @return The anonymous access token of the Crunchyroll API.
     * @see [SmartHttpClient]
     */
    private suspend fun accessToken(): String = accessTokenMutex.withLock {
        val cached = cachedAccessToken
        if (cached != null &&
            Clock.System.now().toEpochMilliseconds() < cached.expiresAtMillis
        ) {
            return cached.token
        }

        val response = client.post<CrunchyrollTokenResponse>(
            url = "$BASE_URL/auth/v1/token",
            body = "grant_type=client_id&client_id=offline_access" +
                "&device_id=$deviceId&device_type=Chrome+on+Linux",
            cacheKey = "crunchyroll:token:$deviceId",
            ttl = TOKEN_TTL,
            contentType = ContentType.Application.FormUrlEncoded,
            headers = mapOf(
                HttpHeaders.Authorization to "Basic $CRUNCHYROLL_BASIC_AUTH_TOKEN",
                "ETP-Anonymous-ID" to deviceId
            )
        )
        val token = response.data

        // A token served from the HTTP cache (revalidation or stale-on-error) may be close to
        // its real expiry, so it is only trusted for half the cache TTL; a fresh response gets
        // its real lifetime minus the safety margin.
        val now = Clock.System.now().toEpochMilliseconds()
        val expiresAtMillis = now +
            if (response.hasChanged) {
                (token.expiresIn - TOKEN_EXPIRY_MARGIN_SECONDS).coerceAtLeast(0) * 1000
            } else {
                TOKEN_TTL.inWholeMilliseconds / 2
            }

        return CachedAccessToken(token.accessToken, expiresAtMillis)
            .also { cachedAccessToken = it }
            .token
    }

    private companion object {
        const val BROWSE_PAGE_SIZE = 25
        const val TOKEN_EXPIRY_MARGIN_SECONDS = 60L
        val TOKEN_TTL = 25.minutes

        // Read by the update-credentials GitHub Actions workflow to detect a newer Android TV
        // APK; the token itself is kept in sync by the same workflow.
        @Suppress("unused")
        const val CRUNCHYROLL_APK_VERSION = "3.76.0_22366"

        // Same token as AbstractCrunchyrollWrapper.CRUNCHYROLL_BASIC_AUTH_TOKEN_DEFAULT of the
        // legacy implementation, kept in sync by the update-credentials GitHub Actions workflow.
        const val CRUNCHYROLL_BASIC_AUTH_TOKEN =
            "ZXI5bmdnMmtvNW9oaXJsYTUxaTI6VmwxVDBQbjhRWko2d3o0NnhYRGxIekRGUDV0Wlgxb3E="
    }
}

/**
 * Cleans a Crunchyroll series title and extracts the forced season it carries.
 *
 * Series titles may embed a season suffix such as `"Series Saison 2"`; the suffix is removed
 * from the returned title and reported as the forced season of the episode.
 *
 * @param title The raw series title of the episode metadata.
 * @return A pair of the cleaned title and the forced season, `null` when no suffix is present.
 * @see [SEASON_REGEX]
 */
private fun cleanAnimeTitle(title: String?): Pair<String, Int?> {
    val raw = title.orEmpty()
    val season = SEASON_REGEX.find(raw)?.groupValues?.get(1)?.toIntOrNull()
    return raw.replace(SEASON_REGEX, "").trim() to season
}

@Serializable
internal data class CrunchyrollTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long = 1800L
)

@Serializable
internal data class CrunchyrollBrowseResponse(
    val data: List<CrunchyrollBrowseObject> = emptyList()
)

@Serializable
internal data class CrunchyrollBrowseObject(
    val id: String = "",
    val title: String? = null,
    val description: String? = null,
    @SerialName("slug_title")
    val slugTitle: String? = null,
    val images: CrunchyrollImages? = null,
    @SerialName("series_metadata")
    val seriesMetadata: CrunchyrollSeries? = null,
    @SerialName("episode_metadata")
    val episodeMetadata: CrunchyrollEpisode? = null
) {
    val fullHDTitle: String
        get() = "https://imgsrv.crunchyroll.com/cdn-cgi/image/format=png,quality=100," +
            "width=1920/keyart/$id-title_logo-en-us"

    /**
     * Maps a Crunchyroll series browse object into the platform anime domain entity.
     *
     * Sanitizes the harvested texts with [sanitizeText], cleans redundant season suffixes from
     * the title with [cleanAnimeTitle], resolves the poster artwork and keys the localized
     * texts by the target [country].
     *
     * @param seriesId The series identifier published as the platform identifier.
     * @param rawTitle The raw series title carried by the episode metadata.
     * @param country Target country distribution, used as the key of the produced localization.
     * @return The normalized [PlatformAnime] domain entity.
     * @see [PlatformAnime]
     * @see [cleanAnimeTitle]
     */
    fun toPlatformAnime(
        seriesId: String,
        rawTitle: String?,
        country: Country
    ): PlatformAnime {
        val (cleanedTitle, _) = cleanAnimeTitle(
            firstNonBlank(rawTitle, seriesMetadata?.title, title)
        )
        val artwork = images ?: seriesMetadata?.images

        return PlatformAnime(
            id = seriesId,
            platform = Platform.CRUNCHYROLL,
            images = listOfNotNull(
                artwork?.fullHDImage?.let { PlatformImage(ImageType.THUMBNAIL, it) },
                artwork?.fullHDBanner?.let { PlatformImage(ImageType.BANNER, it) },
                fullHDTitle.takeIf { id.isNotBlank() }?.let { PlatformImage(ImageType.TITLE, it) }
            ),
            localizations = listOf(
                PlatformAnimeLocalization(
                    country = country,
                    locale = country.defaultLocale,
                    title = cleanedTitle.sanitizeText()
                        ?.takeIf { it.isNotBlank() }
                        ?: seriesId,
                    description = normalizeDescription(
                        firstNonBlank(description, seriesMetadata?.description)
                    ).sanitizeText()
                )
            )
        )
    }

    /**
     * Maps a Crunchyroll episode browse object into a platform episode representation.
     *
     * Applies the release filters of the platform: teasers and epoch-dated entries are rejected
     * with a [TeaserEpisodeException], releases are kept only when they are dubbed or subtitled
     * in the language of the target [country]. The audio locale of the release must resolve to a
     * known [Locale].
     *
     * @param metadata The episode metadata of the browse object.
     * @param anime The parent [PlatformAnime] instance resolved for this episode.
     * @param series The series browse object the episode belongs to.
     * @param country Target country distribution.
     * @return The normalized [PlatformEpisode], or `null` when the release is filtered out.
     * @throws TeaserEpisodeException If the release is a teaser or a promotional video.
     * @throws MissingAudioLocaleException If the audio locale of the release cannot be mapped.
     * @see [PlatformEpisode]
     * @see [Locale]
     */
    fun toPlatformEpisode(
        metadata: CrunchyrollEpisode,
        anime: PlatformAnime,
        series: CrunchyrollBrowseObject,
        country: Country
    ): PlatformEpisode? {
        val releaseDateTime = metadata.releaseDateTime ?: return null

        val isTeaser = listOfNotNull(slugTitle, metadata.slugTitle)
            .any { it.contains(TEASER_REGEX) } ||
            releaseDateTime.toInstant().toEpochMilli() == 0L

        if (isTeaser)
            throw TeaserEpisodeException(
                episodeId = id,
                episodeName = title ?: metadata.title,
                reason = "teaser or promotional video"
            )

        // The dub/subtitle match runs on the language prefix of the country default locale;
        // countries whose default locale has no Crunchyroll counterpart simply match nothing.
        val countryLanguage = country.defaultLocale.code.substringBefore('-')
        val isDubbed = metadata.audioLocale
            ?.substringBefore('-')
            ?.equals(countryLanguage, ignoreCase = true) == true
        val hasSubtitle = metadata.subtitleLocales.orEmpty().any {
            it.substringBefore('-').equals(countryLanguage, ignoreCase = true)
        }

        if (!isDubbed && !hasSubtitle)
            return null

        val audioLocale = Locale.fromCode(metadata.audioLocale.orEmpty())
            ?: throw MissingAudioLocaleException(
                episodeId = id,
                episodeName = title ?: metadata.title,
                languages = listOfNotNull(metadata.audioLocale),
                country = country
            )

        val (number, episodeType) = metadata.getNumberAndEpisodeType()

        val (_, forcedSeason) = cleanAnimeTitle(
            firstNonBlank(metadata.seriesTitle, series.seriesMetadata?.title, series.title)
        )

        val watchSlug = slugTitle?.takeIf { it.isNotBlank() }
            ?: metadata.slugTitle?.takeIf { it.isNotBlank() }
            ?: metadata.seriesSlugTitle?.takeIf { it.isNotBlank() }
        val watchPath = watchSlug?.let { "/$it" }.orEmpty()

        return PlatformEpisode(
            anime = anime,
            id = id,
            image = episodeImage(metadata, series),
            releaseDateTime = releaseDateTime,
            season = forcedSeason ?: (metadata.seasonNumber ?: 1),
            number = number,
            episodeType = episodeType,
            duration = (metadata.durationMs ?: 0L) / 1000L,
            url = "$BASE_URL/${country.code.lowercase()}/watch/$id$watchPath",
            audioLocales = listOf(audioLocale),
            uncensored = metadata.matureBlocked == true,
            localizations = listOf(
                PlatformEpisodeLocalization(
                    country = country,
                    title = firstNonBlank(title, metadata.title).sanitizeText(),
                    description = description.sanitizeText()
                )
            )
        )
    }

    /**
     * Resolves the artwork of an episode, falling back to the series artwork when the episode
     * carries no thumbnail of its own.
     *
     * @param metadata The episode metadata of the browse object.
     * @param series The series browse object the episode belongs to.
     * @return The episode image URL.
     * @see [CrunchyrollImages]
     */
    private fun episodeImage(
        metadata: CrunchyrollEpisode,
        series: CrunchyrollBrowseObject
    ): String {
        val episodeThumbnail =
            images?.fullHDThumbnail ?: metadata.images?.fullHDThumbnail
        if (episodeThumbnail != null)
            return episodeThumbnail

        val seriesImages = series.images ?: series.seriesMetadata?.images
        return seriesImages?.fullHDImage
            ?: seriesImages?.fullHDThumbnail
            ?: seriesImages?.fullHDBanner
            ?: series.fullHDTitle.takeIf { series.id.isNotBlank() }
            ?: ""
    }

    /**
     * Extracts the editorial description of a Crunchyroll text.
     *
     * Crunchyroll descriptions may embed several paragraphs separated by double line breaks;
     * the normalized form keeps only the editorial part of the text.
     *
     * @param description The raw description harvested from the platform.
     * @return The normalized description, or `null` when absent.
     * @see [CrunchyrollBrowseObject.toPlatformAnime]
     */
    private fun normalizeDescription(description: String?): String? =
        description?.split(Regex("""\r?\n\r?\n"""))?.let { lines ->
            when {
                lines.size == 2 -> lines[1]
                lines.size > 2 -> lines.subList(1, lines.size - 1).joinToString(" ")
                else -> lines.firstOrNull()
            }
        }
}

@Serializable
internal data class CrunchyrollImages(
    val thumbnail: List<List<CrunchyrollMediaImage>>? = null,
    @SerialName("poster_tall")
    val posterTall: List<List<CrunchyrollMediaImage>>? = null,
    @SerialName("poster_wide")
    val posterWide: List<List<CrunchyrollMediaImage>>? = null
) {
    val fullHDThumbnail: String?
        get() = thumbnail.orEmpty().firstOrNull()?.maxByOrNull { it.width }?.source
    val fullHDImage: String?
        get() = posterTall.orEmpty().firstOrNull()?.maxByOrNull { it.width }?.source
    val fullHDBanner: String?
        get() = posterWide.orEmpty().firstOrNull()?.maxByOrNull { it.width }?.source
}

@Serializable
internal data class CrunchyrollMediaImage(
    val source: String,
    val width: Int = 0
)

@Serializable
internal data class CrunchyrollSeries(
    val title: String? = null,
    val description: String? = null,
    val images: CrunchyrollImages? = null
)

@Serializable
internal data class CrunchyrollEpisode(
    @SerialName("series_id")
    val seriesId: String? = null,
    @SerialName("series_title")
    val seriesTitle: String? = null,
    @SerialName("series_slug_title")
    val seriesSlugTitle: String? = null,
    @SerialName("audio_locale")
    val audioLocale: String? = null,
    @SerialName("subtitle_locales")
    val subtitleLocales: List<String>? = null,
    @SerialName("premium_available_date")
    val premiumAvailableDate: String? = null,
    @SerialName("availability_starts")
    val availabilityStarts: String? = null,
    @SerialName("season_number")
    val seasonNumber: Int? = null,
    @SerialName("season_slug_title")
    val seasonSlugTitle: String? = null,
    @SerialName("episode")
    val numberString: String? = null,
    @SerialName("episode_number")
    val number: Int? = null,
    val title: String? = null,
    @SerialName("slug_title")
    val slugTitle: String? = null,
    val images: CrunchyrollImages? = null,
    @SerialName("duration_ms")
    val durationMs: Long? = null,
    @SerialName("mature_blocked")
    val matureBlocked: Boolean? = null,
    @SerialName("identifier")
    val identifier: String? = null
) {
    val releaseDateTime: ZonedDateTime?
        get() = listOfNotNull(
            premiumAvailableDate?.takeIf { it.isNotBlank() },
            availabilityStarts?.takeIf { it.isNotBlank() }
        ).firstNotNullOfOrNull { raw ->
            runCatching { ZonedDateTime.parse(raw) }
                .getOrNull()
                ?.takeIf { parsed -> parsed.year < 9000 }
        }

    /**
     * Resolves the number and the editorial type of the episode.
     *
     * Detection rules prioritize films (movie slugs or unnumbered releases longer than one
     * hour), then specials (`SP` numbering, `OAD` identifiers, special season slugs, any
     * unnumbered release, or any non-numeric episode label overriding a recycled numeric
     * `episode_number`), and fall back to standard episodes.
     *
     * @return A pair of the episode number (`-1` when unnumbered) and the [EpisodeType].
     * @see [EpisodeType]
     */
    fun getNumberAndEpisodeType(): Pair<Int, EpisodeType> {
        val trimmedNumberString = numberString?.trim()
        var episodeNumber = number ?: trimmedNumberString?.toIntOrNull() ?: -1

        var episodeType = when {
            seasonSlugTitle?.contains("movie", true) == true ||
                seriesSlugTitle?.contains("movie", true) == true ||
                (episodeNumber == -1 && (durationMs ?: 0L) > 3_600_000L) ->
                EpisodeType.FILM
            episodeNumber == -1 || seasonSlugTitle?.contains("special", true) == true ->
                EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        SPECIAL_EPISODE_REGEX.find(trimmedNumberString.orEmpty())
            ?.groupValues?.get(1)?.toIntOrNull()?.let {
                episodeType = EpisodeType.SPECIAL
                episodeNumber = it
            }

        // A non-numeric episode label (recap, OVA...) is never a standard episode: the numeric
        // episode_number field may be recycled, so the label wins over it.
        if (episodeType == EpisodeType.EPISODE &&
            !trimmedNumberString.isNullOrBlank() &&
            trimmedNumberString.toIntOrNull() == null
        ) {
            episodeType = EpisodeType.SPECIAL
        }

        identifier?.let {
            IDENTIFIER_REGEX.find(it)?.groupValues?.get(1)?.let { part ->
                if (part == "OAD") episodeType = EpisodeType.SPECIAL
            }
        }

        return episodeNumber to episodeType
    }
}
