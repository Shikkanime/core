package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.SmartResponse
import fr.shikkanime.jobs.ZonedDateTimeSerializer
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformAnimeLocalization
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.PlatformEpisodeLocalization
import fr.shikkanime.jobs.platforms.PlatformImage
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.jobs.platforms.exceptions.MissingAudioLocaleException
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.ktor.http.HttpHeaders
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format

import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import java.time.ZonedDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val DIMENSION_REGEX = Regex("""\d+x\d+""")
private val VIDEO_EPS_REGEX = Regex("""/eps$""")
private val LANDSCAPE_WITH_LOGO_REGEX = Regex("""/landscape-with-logo(?:\..*)?$""")
private val SHOW_PORTRAIT_LOGO_REGEX = Regex("""/portrait-with-logo$""")
private val MOVIE_REGEX = Regex("""Film(?: (\d*))?""")
private val SPECIAL_EPISODE_REGEX = Regex("""(?:Épisode spécial|OAV)(?: (\d*))?""")
private val NUMBER_CLEANUP_REGEX = Regex("""\(.*\)""")
private val STATIC_ANIME_NAME_CLEANUP_REGEX = Regex("""(?: -)? Saison \d+|Part .*| [IVXLCDM]+$""")
private val TRAILER_INDICATORS = listOf(
    "Bande-annonce",
    "Bande annonce",
    "Court-métrage",
    "Opening",
    "Making-of"
)
private val SPECIAL_SHOW_TYPES = setOf(AdnType.PV, AdnType.BONUS)

/**
 * Resolves the canonical audio locale for a release given the target country and ADN language code.
 *
 * For Francophone countries (FR, BE, LU, CH, MC, AD), maps `"vostf"` to [Locale.JA_JP]
 * and `"vf"` to [Locale.FR_FR].
 * For Germany (DE), maps `"vostde"` to [Locale.JA_JP] and `"vde"` to [Locale.DE_DE].
 *
 * Any language code unsupported or outside the target country distribution returns `null`.
 *
 * @param country Target distribution country.
 * @param language ADN language identifier tag (e.g. "vostf", "vf", "vostde", "vde").
 * @return Canonical [Locale] instance, or `null` if unmapped for the country.
 * @see [Country]
 * @see [Locale]
 */
internal fun getAudioLocale(country: Country, language: String): Locale? =
    when (country) {
        Country.DE -> when (language) {
            "vostde" -> Locale.JA_JP
            "vde" -> Country.DE.defaultLocale
            else -> null
        }
        else -> when (language) {
            "vostf" -> Locale.JA_JP
            "vf" -> Country.FR.defaultLocale
            else -> null
        }
    }

/**
 * Sanitizes and normalizes an anime show title by removing redundant season tags, part labels,
 * trailing season numbers, and Roman numerals.
 *
 * This normalization ensures consistent grouping of episodes under a unified [PlatformAnime] title,
 * regardless of platform-specific naming variations (e.g. "My Hero Academia Saison 6" -> "My Hero Academia").
 *
 * @param title The raw anime title string provided by the platform (e.g. from [AdnShow.title]).
 * @param season The broadcast season number extracted for the episode, used to trim trailing numbers.
 * @return The cleaned and trimmed anime title suitable for [PlatformAnimeLocalization.title].
 * @see [PlatformAnime]
 * @see [AdnShow.toPlatformAnime]
 */
internal fun cleanAnimeName(title: String, season: Int): String {
    val withoutStatic = title.replace(STATIC_ANIME_NAME_CLEANUP_REGEX, "")
    val withoutSeason = if (season > 0)
        withoutStatic.removeSuffix(" $season")
    else
        withoutStatic
    return withoutSeason.trim()
}

/**
 * Evaluates whether a video entry represents a feature film or movie release.
 *
 * Checks both explicit title regex matching and platform metadata types ([AdnType.MOV]).
 *
 * @param movieMatch The optional regex [MatchResult] obtained by searching for movie indicators in title.
 * @param videoType The [AdnType] declared directly on the [AdnVideo].
 * @param showType The [AdnType] declared on the parent [AdnShow].
 * @return `true` if the media should be classified as a film, `false` otherwise.
 * @see [AdnType]
 * @see [EpisodeType.FILM]
 */
private fun isMovie(
    movieMatch: MatchResult?,
    videoType: AdnType,
    showType: AdnType
): Boolean =
    movieMatch != null
        || videoType == AdnType.MOV
        || showType == AdnType.MOV

/**
 * Evaluates whether a video entry represents a special episode, OVA, or recap release.
 *
 * Checks explicit regex match results, platform metadata types ([AdnType.OAV]), or non-integer
 * fractional episode numbering strings (e.g. "12.5").
 *
 * @param specialMatch The optional regex [MatchResult] for special episode keywords.
 * @param videoType The [AdnType] declared directly on the [AdnVideo].
 * @param showType The [AdnType] declared on the parent [AdnShow].
 * @param rawEpisodeString The raw episode order string from [AdnVideo.shortNumber].
 * @return `true` if the media should be classified as a special episode or OVA, `false` otherwise.
 * @see [AdnType]
 * @see [EpisodeType.SPECIAL]
 */
private fun isSpecial(
    specialMatch: MatchResult?,
    videoType: AdnType,
    showType: AdnType,
    rawEpisodeString: String?
): Boolean =
    specialMatch != null
        || videoType == AdnType.OAV
        || showType == AdnType.OAV
        || rawEpisodeString?.contains(".") == true

/**
 * Analyzes the raw episode identifier string and platform types to determine the numerical sequence
 * number and categorize the release into an [EpisodeType].
 *
 * Detection rules prioritize:
 * 1. Films ([EpisodeType.FILM]): via regex match or [AdnType.MOV] on video or parent show.
 * 2. Specials ([EpisodeType.SPECIAL]): via regex match, [AdnType.OAV], or decimal numbering.
 * 3. Standard episodes ([EpisodeType.EPISODE]): fallback when neither film nor special rules apply.
 *
 * @param rawEpisodeString The raw sequence string (e.g. "12", "Film 2", "OAV 1", "12.5").
 * @param videoType The [AdnType] declared on the individual [AdnVideo].
 * @param showType The [AdnType] declared on the parent [AdnShow].
 * @return A [Pair] containing the resolved episode sequence number (-1 if unnumbered) and [EpisodeType].
 * @see [EpisodeType]
 * @see [AdnType]
 * @see [parseInitialNumber]
 */
internal fun getNumberAndEpisodeType(
    rawEpisodeString: String?,
    videoType: AdnType,
    showType: AdnType
): Pair<Int, EpisodeType> {
    val initialNumber = parseInitialNumber(rawEpisodeString)
    val movieMatch = rawEpisodeString?.let(MOVIE_REGEX::find)

    if (isMovie(movieMatch, videoType, showType)) {
        val filmNumber = movieMatch?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return (filmNumber ?: initialNumber) to EpisodeType.FILM
    }

    val specialMatch = rawEpisodeString?.let(SPECIAL_EPISODE_REGEX::find)

    if (isSpecial(specialMatch, videoType, showType, rawEpisodeString)) {
        val specialNumber = specialMatch?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return (specialNumber ?: initialNumber) to EpisodeType.SPECIAL
    }

    return initialNumber to EpisodeType.EPISODE
}

/**
 * Extracts the primary numerical sequence number from a raw episode string, ignoring bracketed qualifiers.
 *
 * For example, strings such as `"12 (vostf)"` or `"5"` will yield `12` and `5` respectively.
 * If the string cannot be parsed into an integer, `-1` is returned to indicate an unnumbered entry.
 *
 * @param rawString The raw string representation of the episode number from [AdnVideo.shortNumber].
 * @return The parsed positive integer sequence number, or `-1` if absent, blank, or non-numeric.
 * @see [getNumberAndEpisodeType]
 */
internal fun parseInitialNumber(rawString: String?): Int =
    rawString?.replace(NUMBER_CLEANUP_REGEX, "")
        ?.trim()
        ?.toIntOrNull() ?: -1

/**
 * Streaming platform connector for Animation Digital Network (ADN).
 *
 * This implementation connects to the ADN public calendar REST API, retrieves daily video releases,
 * filters out non-animation and promotional content, normalizes metadata into domain representations,
 * and handles multi-language audio track fan-out (VOSTFR as `ja-JP` and VF as `fr-FR`).
 *
 * @param client The [SmartHttpClient] instance used to execute cached HTTP requests against the ADN API.
 * @param ttl The cache time-to-live [Duration] for release calendar responses (defaults to 1 minute).
 * @param countries The target countries enabled for this platform connector.
 * @see [StreamingPlatform]
 * @see [PlatformEpisode]
 * @see [PlatformAnime]
 * @see [Platform.ANIMATION_DIGITAL_NETWORK]
 */
@Single(binds = [StreamingPlatform::class])
class AnimationDigitalNetworkPlatform(
    private val client: SmartHttpClient,
    private val ttl: Duration = 1.minutes,
    override val countries: Set<Country> = setOf(Country.FR)
) : StreamingPlatform {
    override val platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK

    private val logger = LoggerFactory.getLogger()
    private var cachedEpisodes: List<PlatformEpisode> = emptyList()
    private val cachedEpisodesByCountry = mutableMapOf<Country, List<PlatformEpisode>>()

    /**
     * Retrieves the latest episodes released today on the ADN platform across configured countries.
     *
     * Constructs the current date in UTC ISO-8601 format, queries the ADN video calendar endpoint
     * for each country declared in [countries], and maps valid releases into [PlatformEpisode] instances.
     *
     * In-memory cache is returned if [SmartHttpClient] reports that the remote payload has not changed.
     *
     * @return A [SmartResponse] carrying the normalized [PlatformEpisode] objects representing
     * today's releases and the change flag of the ADN payload.
     * @throws io.ktor.client.plugins.ResponseException If the ADN API responds with an unhandled HTTP error.
     * @see [SmartResponse]
     * @see [PlatformEpisode]
     * @see [AdnVideo.toPlatformEpisode]
     */
    override suspend fun fetchLatestEpisodes(): SmartResponse<List<PlatformEpisode>> {
        if (countries.isEmpty())
            return SmartResponse(emptyList(), hasChanged = false)

        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
            .date
            .format(LocalDate.Formats.ISO)

        var hasChanged = false

        val episodesByCountry = countries.flatMap { country ->
            val (episodes, changed) = fetchEpisodesForCountry(now, country)
            if (changed)
                hasChanged = true
            episodes
        }

        if (hasChanged) {
            cachedEpisodes = episodesByCountry
                .groupBy { it.id }
                .values
                .map { group ->
                    val first = group.first()
                    val allAudioLocales = group.flatMap { it.audioLocales }.distinct()
                    val allLocalizations = group.flatMap { it.localizations }.distinct()
                    first.copy(
                        audioLocales = allAudioLocales,
                        localizations = allLocalizations
                    )
                }
        }

        return SmartResponse(cachedEpisodes, hasChanged)
    }

    /**
     * Converts a list of [AdnVideo] entries into domain [PlatformEpisode] objects for a given [country].
     *
     * Applies filtering against promotional and invalid content, resolves parent anime instances,
     * and maps localized audio tracks and descriptions.
     *
     * @param videos The raw ADN video list from the calendar response.
     * @param country Target country distribution.
     * @return A mapped list of [PlatformEpisode] objects.
     * @see [AdnVideo]
     * @see [PlatformEpisode]
     * @see [Country]
     */
    internal fun convertVideos(
        videos: List<AdnVideo>,
        country: Country
    ): List<PlatformEpisode> {
        val animes = mutableMapOf<Int, PlatformAnime>()
        val invalidShowIds = mutableSetOf<Int>()

        return videos.mapNotNull { video ->
            if (!video.isValid)
                return@mapNotNull null
            val anime = resolveAnime(video.show, animes, invalidShowIds, video.parsedSeason, country)
                ?: return@mapNotNull null

            try {
                video.toPlatformEpisode(anime, country)
            } catch (e: MissingAudioLocaleException) {
                logger.warning(e.message)
                null
            }
        }
    }

    /**
     * Fetches and maps ADN calendar releases for a specific country.
     *
     * Injects localization headers ([HttpHeaders.AcceptLanguage], `X-Source`, and `X-Target-Distribution`)
     * matching the country distribution, querying the video calendar and mapping all valid releases.
     *
     * @param now Current date in ISO-8601 format.
     * @param country Target country distribution.
     * @return Cached or freshly mapped list of [PlatformEpisode] instances for this country and changed flag.
     * @see [Country]
     * @see [PlatformEpisode]
     */
    private suspend fun fetchEpisodesForCountry(
        now: String,
        country: Country
    ): Pair<List<PlatformEpisode>, Boolean> {
        val distribution = country.code.lowercase()
        val language = country.defaultLocale.code
            .substringBefore('-')
            .lowercase()
        val cacheKey = "animation_digital_network:calendar:$now:$distribution"

        val response = client.get<AdnCalendarResponse>(
            "https://gw.api.animationdigitalnetwork.com/video/calendar?date=$now",
            cacheKey,
            ttl,
            headers = mapOf(
                HttpHeaders.AcceptLanguage to language,
                "X-Source" to "Web",
                "X-Target-Distribution" to distribution,
            ),
        )

        val hasChanged = response.hasChanged
            || country !in cachedEpisodesByCountry

        if (hasChanged)
            cachedEpisodesByCountry[country] = convertVideos(response.data.videos, country)

        return cachedEpisodesByCountry[country].orEmpty() to hasChanged
    }

    /**
     * Resolves and caches the [PlatformAnime] instance corresponding to an [AdnShow], skipping non-animation.
     *
     * Maintains a local cache ([animes]) across the current calendar batch to prevent duplicate show
     * conversions, and tracks invalid non-animation IDs ([invalidShowIds]) to short-circuit lookups.
     *
     * @param show The raw [AdnShow] metadata from the calendar response.
     * @param animes Mutable cache mapping ADN show IDs to already resolved [PlatformAnime] instances.
     * @param invalidShowIds Set of show IDs identified as non-animation or invalid during the current batch.
     * @param season The broadcast season index associated with the current video release.
     * @param country Target country distribution, used as the key of the anime localization.
     * @return The resolved [PlatformAnime] instance, or `null` if the show is not an animation title.
     * @see [PlatformAnime]
     * @see [AdnShow.toPlatformAnime]
     */
    private fun resolveAnime(
        show: AdnShow,
        animes: MutableMap<Int, PlatformAnime>,
        invalidShowIds: MutableSet<Int>,
        season: Int,
        country: Country
    ): PlatformAnime? {
        if (show.id in invalidShowIds)
            return null

        return animes[show.id] ?: run {
            if (!show.isAnimation) {
                invalidShowIds.add(show.id)
                return null
            }

            show.toPlatformAnime(season, country)
                .also { animes[show.id] = it }
        }
    }
}

@Serializable
internal data class AdnCalendarResponse(
    val videos: List<AdnVideo>
)

internal enum class AdnType {
    EPS,
    MOV,
    OAV,
    PV,
    BONUS
}

@Serializable
internal data class AdnVideo(
    val id: Int,
    val name: String,
    val title: String?,
    val shortNumber: String,
    val season: String?,
    val type: AdnType,
    private val image2x: String,
    val summary: String?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val releaseDate: ZonedDateTime,
    val duration: Long,
    val url: String,
    val languages: List<String>,
    val show: AdnShow
) {
    val image: String by lazy(LazyThreadSafetyMode.NONE) {
        image2x.replace(DIMENSION_REGEX, "1920x1080")
            .replace(VIDEO_EPS_REGEX, "/eps.width=1920,height=1080,quality=100")
            .replace(
                LANDSCAPE_WITH_LOGO_REGEX,
                "/landscape-with-logo.width=1920,height=1080,quality=100"
            )
    }

    val isValid: Boolean
        get() =
            type !in SPECIAL_SHOW_TYPES
                && TRAILER_INDICATORS.none { shortNumber.startsWith(it) }

    val isUncensored: Boolean
        get() =
            title?.let {
                it.contains("(NC)", ignoreCase = true)
                    || it.contains("Non censuré", ignoreCase = true)
            } ?: false

    val parsedSeason: Int
        get() =
            season?.toIntOrNull() ?: 1

    /**
     * Converts an ADN video and its parent show into a platform episode representation.
     *
     * Resolves audio locales dynamically for [country]. Produces a [PlatformEpisode] with its
     * localized metadata for the given [country].
     *
     * @param anime The parent [PlatformAnime] instance previously resolved for this episode.
     * @param country Target country distribution.
     * @return The normalized [PlatformEpisode].
     * @throws MissingAudioLocaleException If no audio locales can be resolved for [country].
     * @see [PlatformEpisode]
     * @see [PlatformAnime]
     * @see [getNumberAndEpisodeType]
     */
    fun toPlatformEpisode(
        anime: PlatformAnime,
        country: Country
    ): PlatformEpisode {
        val (number, episodeType) = getNumberAndEpisodeType(shortNumber, type, show.type)
        val audioLocales = languages.mapNotNull { getAudioLocale(country, it) }
            .distinct()

        if (audioLocales.isEmpty())
            throw MissingAudioLocaleException(
                episodeId = id.toString(),
                episodeName = title,
                languages = languages,
                country = country
            )

        return PlatformEpisode(
            anime = anime,
            id = id.toString(),
            image = image,
            releaseDateTime = releaseDate,
            season = parsedSeason,
            number = number,
            episodeType = episodeType,
            duration = duration,
            url = url,
            audioLocales = audioLocales,
            uncensored = isUncensored,
            localizations = listOf(
                PlatformEpisodeLocalization(
                    country = country,
                    title = name,
                    description = summary
                )
            )
        )
    }
}

@Serializable
internal data class AdnShow(
    val id: Int,
    val title: String,
    val shortTitle: String?,
    val originalTitle: String?,
    val summary: String?,
    private val image2x: String,
    private val imageHorizontal2x: String,
    val type: AdnType,
    val genres: List<String>
) {
    val thumbnail: String by lazy(LazyThreadSafetyMode.NONE) {
        image2x.replace(DIMENSION_REGEX, "1080x1543")
            .replace(
                SHOW_PORTRAIT_LOGO_REGEX,
                "/portrait-with-logo.width=1080,height=1543,quality=100"
            )
    }

    val banner: String by lazy(LazyThreadSafetyMode.NONE) {
        imageHorizontal2x.replace(DIMENSION_REGEX, "1920x1080")
            .replace(
                LANDSCAPE_WITH_LOGO_REGEX,
                "/landscape-with-logo.width=1920,height=1080,quality=100"
            )
    }

    val isAnimation: Boolean
        get() =
            genres.any { it.contains("Animation ") }

    /**
     * Maps an ADN show into the platform anime domain entity.
     *
     * Cleans redundant season suffixes and part labels from the title using [cleanAnimeName],
     * strips newlines from the description, resolves high-resolution artwork, and keys the
     * localized texts by the target [country].
     *
     * @param season The broadcast season index used to clean trailing season numbers from the title.
     * @param country Target country distribution, used as the key of the produced localization.
     * @return The normalized [PlatformAnime] domain entity.
     * @see [PlatformAnime]
     * @see [cleanAnimeName]
     */
    fun toPlatformAnime(season: Int, country: Country): PlatformAnime {
        val rawTitle = shortTitle?.takeIf { it.isNotBlank() } ?: title
        val cleanedTitle = cleanAnimeName(rawTitle, season)

        return PlatformAnime(
            id = id.toString(),
            platform = Platform.ANIMATION_DIGITAL_NETWORK,
            images = listOf(
                PlatformImage(type = ImageType.THUMBNAIL, url = thumbnail),
                PlatformImage(type = ImageType.BANNER, url = banner)
            ),
            localizations = listOf(
                PlatformAnimeLocalization(
                    country = country,
                    locale = country.defaultLocale,
                    title = cleanedTitle,
                    description = summary?.replace("\n", "")
                        ?.replace(" ", " ")
                )
            )
        )
    }
}
