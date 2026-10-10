package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.ZonedDateTimeSerializer
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.models.EpisodeType
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
private val TRAILER_INDICATORS = listOf(
    "Bande-annonce",
    "Bande annonce",
    "Court-métrage",
    "Opening",
    "Making-of"
)
private val SPECIAL_SHOW_TYPES = setOf(AdnType.PV, AdnType.BONUS)
private val LOCALE_MAP = mapOf(
    "vostf" to "ja-JP",
    "vf" to "fr-FR"
)

/**
 * Sanitizes and normalizes an anime show title by removing redundant season tags, part labels,
 * trailing season numbers, and Roman numerals.
 *
 * This normalization ensures consistent grouping of episodes under a unified [PlatformAnime] title,
 * regardless of platform-specific naming variations (e.g. "My Hero Academia Saison 6" -> "My Hero Academia").
 *
 * @param title The raw anime title string provided by the platform (e.g. from [AdnShow.title]).
 * @param season The broadcast season number extracted for the episode, used to trim trailing numbers.
 * @return The cleaned and trimmed anime title suitable for [PlatformAnime.title].
 * @see PlatformAnime
 * @see AdnShow.toPlatformAnime
 */
internal fun cleanAnimeName(title: String, season: Int): String {
    val regex = Regex("""(?: -)? Saison \d+|Part .*| $season$| [IVXLCDM]+$""")
    return title.replace(regex, "")
        .trim()
}

/**
 * Evaluates whether a video entry represents a feature film or movie release.
 *
 * Checks both explicit title regex matching and platform metadata types ([AdnType.MOV]).
 *
 * @param movieMatch The optional regex [MatchResult] obtained by searching for movie indicators in title.
 * @param videoType The [AdnType] declared directly on the [AdnVideo].
 * @param showType The optional [AdnType] declared on the parent [AdnShow].
 * @return `true` if the media should be classified as a film, `false` otherwise.
 * @see AdnType
 * @see EpisodeType.FILM
 */
private fun isMovie(
    movieMatch: MatchResult?,
    videoType: AdnType,
    showType: AdnType?
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
 * @param showType The optional [AdnType] declared on the parent [AdnShow].
 * @param rawEpisodeString The raw episode order string from [AdnVideo.shortNumber].
 * @return `true` if the media should be classified as a special episode or OVA, `false` otherwise.
 * @see AdnType
 * @see EpisodeType.SPECIAL
 */
private fun isSpecial(
    specialMatch: MatchResult?,
    videoType: AdnType,
    showType: AdnType?,
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
 * @param showType The optional [AdnType] declared on the parent [AdnShow].
 * @return A [Pair] containing the resolved episode sequence number (-1 if unnumbered) and [EpisodeType].
 * @see EpisodeType
 * @see AdnType
 * @see parseInitialNumber
 */
internal fun getNumberAndEpisodeType(
    rawEpisodeString: String?,
    videoType: AdnType,
    showType: AdnType?
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
 * ### Usage
 * In a Koin-managed environment, inject this class as a [StreamingPlatform] implementation:
 * ```
 * val adnPlatform: StreamingPlatform by inject()
 * val episodes: List<PlatformEpisode> = adnPlatform.fetchLatestEpisodes()
 * ```
 *
 * The connector relies on [SmartHttpClient] with conditional requests (ETag / HTTP 304) and internal
 * in-memory caching to minimize outbound bandwidth when the calendar has not changed.
 *
 * @param client The [SmartHttpClient] instance used to execute cached HTTP requests against the ADN API.
 * @param ttl The cache time-to-live [Duration] for release calendar responses (defaults to 1 minute).
 * @see StreamingPlatform
 * @see PlatformEpisode
 * @see PlatformAnime
 * @see Platform.ANIMATION_DIGITAL_NETWORK
 */
@Single(binds = [StreamingPlatform::class])
class AnimationDigitalNetworkPlatform(
    private val client: SmartHttpClient,
    private val ttl: Duration = 1.minutes
) : StreamingPlatform {
    override val platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK

    private var cachedEpisodes: List<PlatformEpisode> = emptyList()

    /**
     * Retrieves the latest episodes released today on the ADN platform.
     *
     * Constructs the current date in UTC ISO-8601 format, queries the ADN video calendar endpoint,
     * and maps each valid video entry into one or more [PlatformEpisode] instances depending on the
     * available audio languages (VOSTFR and/or VF).
     *
     * An in-memory cache is returned if [SmartHttpClient] reports that the remote payload has not changed.
     *
     * @return A [List] of normalized [PlatformEpisode] objects representing today's releases. Returns an
     * empty list if no releases occurred today or if all entries were filtered out (e.g. live-action).
     * @throws io.ktor.client.plugins.ResponseException If the ADN API responds with an unhandled HTTP error.
     * @see PlatformEpisode
     * @see AdnVideo.toPlatformEpisodes
     */
    override suspend fun fetchLatestEpisodes(): List<PlatformEpisode> {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
            .date
            .format(LocalDate.Formats.ISO)

        val response = client.get<AdnCalendarResponse>(
            "https://gw.api.animationdigitalnetwork.com/video/calendar?date=$now",
            "animation_digital_network:calendar:$now",
            ttl,
            headers = mapOf(
                HttpHeaders.AcceptLanguage to "fr",
                "X-Source" to "Web",
                "X-Target-Distribution" to "fr",
            )
        )

        if (response.hasChanged) {
            val animes = mutableMapOf<Int, PlatformAnime>()
            val invalidShowIds = mutableSetOf<Int>()

            cachedEpisodes = response.data.videos.flatMap { video ->
                if (!video.isValid) return@flatMap emptyList()
                val anime = resolveAnime(video.show, animes, invalidShowIds, video.parsedSeason)
                    ?: return@flatMap emptyList()
                video.toPlatformEpisodes(anime)
            }
        }

        return cachedEpisodes
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
     * @return The resolved [PlatformAnime] instance, or `null` if the show is not an animation title.
     * @see PlatformAnime
     * @see AdnShow.toPlatformAnime
     */
    private fun resolveAnime(
        show: AdnShow,
        animes: MutableMap<Int, PlatformAnime>,
        invalidShowIds: MutableSet<Int>,
        season: Int
    ): PlatformAnime? {
        if (show.id in invalidShowIds) return null

        return animes[show.id] ?: run {
            if (!show.isAnimation) {
                invalidShowIds.add(show.id)
                return null
            }

            show.toPlatformAnime(season)
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
    /**
     * Resolves the high-resolution 1920x1080 thumbnail image URL derived from the 2x preview source.
     */
    val image: String by lazy(LazyThreadSafetyMode.NONE) {
        image2x.replace(DIMENSION_REGEX, "1920x1080")
            .replace(VIDEO_EPS_REGEX, "/eps.width=1920,height=1080,quality=100")
            .replace(
                LANDSCAPE_WITH_LOGO_REGEX,
                "/landscape-with-logo.width=1920,height=1080,quality=100"
            )
    }

    /**
     * Indicates whether the video is an active episode and not a promotional or bonus video.
     */
    val isValid: Boolean
        get() =
            type !in SPECIAL_SHOW_TYPES
                && TRAILER_INDICATORS.none { shortNumber.startsWith(it) }

    /**
     * Indicates whether the episode is an uncensored release based on title and summary flags.
     */
    val isUncensored: Boolean
        get() =
            title?.let {
                it.contains("(NC)", ignoreCase = true)
                    || it.contains("Non censuré", ignoreCase = true)
            } ?: false

    /**
     * Extracts the broadcast season number from the raw season string, defaulting to 1.
     */
    val parsedSeason: Int
        get() =
            season?.toIntOrNull() ?: 1

    /**
     * Converts an ADN video and its parent show into platform episode representations across languages.
     *
     * Maps `"vostf"` to audio locale `ja-JP` and `"vf"` to `fr-FR`. Each language track produces a distinct
     * [PlatformEpisode], with [PlatformEpisode.original] set to `true` for the primary audio track.
     *
     * @param anime The parent [PlatformAnime] instance previously resolved for this episode.
     * @return A list of [PlatformEpisode] instances, one per supported audio language track.
     * @see PlatformEpisode
     * @see PlatformAnime
     * @see getNumberAndEpisodeType
     */
    fun toPlatformEpisodes(anime: PlatformAnime): List<PlatformEpisode> {
        val (number, episodeType) = getNumberAndEpisodeType(shortNumber, type, show.type)
        val audioLanguages = languages.ifEmpty { listOf("vostf") }

        return audioLanguages.mapNotNull { lang ->
            val audioLocale = LOCALE_MAP[lang] ?: return@mapNotNull null
            val isOriginal =
                audioLanguages.size <= 1
                    || audioLanguages.indexOf(lang) == 0

            PlatformEpisode(
                anime = anime,
                id = id.toString(),
                title = name,
                description = summary,
                image = image,
                releaseDateTime = releaseDate,
                season = parsedSeason,
                number = number,
                episodeType = episodeType,
                duration = duration,
                url = url,
                audioLocale = audioLocale,
                uncensored = isUncensored,
                original = isOriginal
            )
        }
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
    private val imageHorizontal2x: String?,
    val type: AdnType? = null,
    val genres: List<String>
) {
    /**
     * Resolves the high-resolution portrait poster image URL with title logo.
     */
    val thumbnail: String by lazy(LazyThreadSafetyMode.NONE) {
        image2x.replace(DIMENSION_REGEX, "1080x1543")
            .replace(
                SHOW_PORTRAIT_LOGO_REGEX,
                "/portrait-with-logo.width=1080,height=1543,quality=100"
            )
    }

    /**
     * Resolves the high-resolution landscape banner image URL with title logo, if available.
     */
    val banner: String? by lazy(LazyThreadSafetyMode.NONE) {
        imageHorizontal2x?.replace(DIMENSION_REGEX, "1920x1080")
            ?.replace(
                LANDSCAPE_WITH_LOGO_REGEX,
                "/landscape-with-logo.width=1920,height=1080,quality=100"
            )
    }

    /**
     * Indicates whether the show belongs to the animation genre.
     */
    val isAnimation: Boolean
        get() =
            genres.any { it.contains("Animation ") }

    /**
     * Maps an ADN show into the platform anime domain entity.
     *
     * Cleans redundant season suffixes and part labels from the title using [cleanAnimeName],
     * strips newlines from the description, and resolves high-resolution [thumbnail] and [banner] artwork.
     *
     * @param season The broadcast season index used to clean trailing season numbers from the title.
     * @return The normalized [PlatformAnime] domain entity.
     * @see PlatformAnime
     * @see cleanAnimeName
     */
    fun toPlatformAnime(season: Int): PlatformAnime {
        val rawTitle = shortTitle?.takeIf { it.isNotBlank() } ?: title
        val cleanedTitle = cleanAnimeName(rawTitle, season)

        return PlatformAnime(
            id = id.toString(),
            title = cleanedTitle,
            description = summary?.replace("\n", ""),
            thumbnail = thumbnail,
            banner = banner
        )
    }
}
