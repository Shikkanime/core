package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.ZonedDateTimeSerializer
import fr.shikkanime.jobs.diagnostics.IngestionRun
import fr.shikkanime.jobs.diagnostics.IngestionVerdict
import fr.shikkanime.jobs.diagnostics.PlatformItem
import fr.shikkanime.jobs.diagnostics.RejectionReason
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.models.Platform
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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
private val LANDSCAPE_WITH_LOGO_REGEX = Regex("""/landscape-with-logo$""")
private val SHOW_PORTRAIT_LOGO_REGEX = Regex("""/portrait-with-logo$""")
private val TRAILER_INDICATORS = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
private val SPECIAL_SHOW_TYPES = setOf(AdnVideoType.PV, AdnVideoType.BONUS)

/**
 * Reads episodes from the Animation Digital Network calendar endpoint.
 *
 * @property client HTTP client used for the calendar call, injected.
 * @property ttl How long a calendar response stays fresh in the client cache.
 */
@Single(binds = [StreamingPlatform::class])
class AnimationDigitalNetworkPlatform(
    private val client: SmartHttpClient,
    private val ttl: Duration = 1.minutes
) : StreamingPlatform {
    override val platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK
    private var cachedEpisodes: List<PlatformEpisode> = emptyList()

    /**
     * Returns the episodes the calendar currently exposes.
     *
     * The mapping is redone only when the calendar content changed; on an unchanged payload the
     * previously mapped list is returned as-is, so a job tick costs one cached read.
     */
    override suspend fun fetchLatestEpisodes(): List<PlatformEpisode> {
        val response = client.get<AdnCalendarResponse>(
            "https://gw.api.animationdigitalnetwork.com/video/calendar?date=${today()}",
            "animation_digital_network:calendar:${today()}",
            ttl,
            headers = CALENDAR_HEADERS
        )

        if (response.hasChanged) {
            cachedEpisodes = diagnoseVideos(response.data.videos)
                .mapNotNull { (it as? IngestionVerdict.Accepted)?.episode }
        }

        return cachedEpisodes
    }

    /**
     * Runs the mapping over every video the calendar returned, keeping the rejected ones so the
     * caller can see which rule dropped them.
     *
     * Unlike [fetchLatestEpisodes] this ignores the `hasChanged` gate: a diagnostic that only
     * described a fresh payload would report nothing on every unchanged tick. A failing fetch is
     * reported as a run carrying an error instead of being thrown, so the failure stays visible
     * rather than leaving the previous run in place looking fresh.
     */
    override suspend fun diagnoseLatestEpisodes(): IngestionRun {
        val now = now()

        val response = try {
            client.get<AdnCalendarResponse>(
                "https://gw.api.animationdigitalnetwork.com/video/calendar?date=${today()}",
                "animation_digital_network:calendar:${today()}",
                ttl,
                headers = CALENDAR_HEADERS
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return IngestionRun(
                platform = platform,
                verdicts = emptyList(),
                fetchedAt = now,
                error = "${exception::class.simpleName}: ${exception.message}"
            )
        }

        return IngestionRun(
            platform = platform,
            verdicts = diagnoseVideos(response.data.videos),
            fetchedAt = now
        )
    }

    private fun today(): String =
        Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
            .date
            .format(LocalDate.Formats.ISO)

    private fun now(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.UTC)

    /**
     * Maps every video to exactly one verdict, resolved anime included. A show already known to
     * be invalid is still given a verdict per video, otherwise the memoized path would silently
     * drop the episodes that come after the first one.
     */
    private fun diagnoseVideos(videos: List<AdnVideo>): List<IngestionVerdict> {
        val animes = mutableMapOf<Int, PlatformAnime>()
        val invalidShowReasons = mutableMapOf<Int, RejectionReason>()

        return videos.map { video ->
            rejectVideo(video)
                ?: rejectTrailer(video)
                ?: rejectShow(video, animes, invalidShowReasons)
                ?: IngestionVerdict.accepted(
                    item = video.toPlatformItem(),
                    episode = video.toPlatformEpisode(resolveAnime(video.show, animes))
                )
        }
    }

    /**
     * Returns the memoized anime for [show], converting it on first sight so every video of the
     * same show shares one instance instead of rebuilding it.
     */
    private fun resolveAnime(show: AdnShow, animes: MutableMap<Int, PlatformAnime>): PlatformAnime =
        animes.getOrPut(show.id) { show.toPlatformAnime() }

    /**
     * Rejects a promotional or bonus video. Checked before the trailer rule because a PV is
     * dropped for being a PV whatever its short number says.
     */
    private fun rejectVideo(video: AdnVideo): IngestionVerdict.Rejected? =
        video.type.takeIf { it in SPECIAL_SHOW_TYPES }?.let {
            IngestionVerdict.rejected(
                item = video.toPlatformItem(),
                reason = RejectionReason.PROMOTIONAL_CONTENT,
                evidence = "type=${video.type}"
            )
        }

    /**
     * Rejects a trailer, opening or making-of, matched on the short number prefix. ADN labels
     * these in `shortNumber` rather than in a dedicated field.
     */
    private fun rejectTrailer(video: AdnVideo): IngestionVerdict.Rejected? =
        TRAILER_INDICATORS.firstOrNull { video.shortNumber.startsWith(it) }?.let {
            IngestionVerdict.rejected(
                item = video.toPlatformItem(),
                reason = RejectionReason.TRAILER_OR_OPENING,
                evidence = "shortNumber=${video.shortNumber}"
            )
        }

    /**
     * Rejects on the show's own data, memoizing the reason per show id so a second video of the
     * same show reports that same reason instead of re-deriving a possibly different one from
     * its own copy of the payload.
     */
    private fun rejectShow(
        video: AdnVideo,
        animes: MutableMap<Int, PlatformAnime>,
        invalidShowReasons: MutableMap<Int, RejectionReason>
    ): IngestionVerdict.Rejected? {
        val show = video.show

        invalidShowReasons[show.id]?.let { memoizedReason ->
            return IngestionVerdict.rejected(
                video.toPlatformItem(),
                reason = memoizedReason,
                evidence = "show id=${show.id} rejected earlier for the same reason (genres=${show.genres})"
            )
        }

        if (show.id in animes) return null

        val reason = show.rejectionReason() ?: return null
        invalidShowReasons[show.id] = reason

        return IngestionVerdict.rejected(
            video.toPlatformItem(),
            reason = reason,
            evidence = "genres=${show.genres}"
        )
    }

    private companion object {
        private val CALENDAR_HEADERS = mapOf(
            HttpHeaders.AcceptLanguage to "fr",
            "X-Source" to "Web",
            "X-Target-Distribution" to "fr"
        )
    }
}

/**
 * Raw ADN calendar payload.
 *
 * @property videos The videos published for the requested day and the following ones.
 */
@Serializable
internal data class AdnCalendarResponse(
    val videos: List<AdnVideo>
)

/** Kind of content an ADN video carries. */
internal enum class AdnVideoType {
    EPS,
    PV,
    BONUS
}

/**
 * A single ADN video.
 *
 * @property id Identifier of the video on ADN.
 * @property name Display name of the episode.
 * @property shortNumber ADN's own numbering label, which also flags trailers.
 * @property type Kind of content this video carries.
 * @property summary Episode summary, nullable on the platform.
 * @property releaseDate When ADN publishes the episode.
 * @property show The show the episode belongs to.
 */
@Serializable
internal data class AdnVideo(
    val id: Int,
    val name: String,
    val shortNumber: String,
    val type: AdnVideoType,
    private val image2x: String,
    val summary: String?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val releaseDate: ZonedDateTime,
    val show: AdnShow
) {
    /**
     * High-definition 1920x1080 thumbnail URL derived from the 2x source image.
     */
    val image: String by lazy(LazyThreadSafetyMode.NONE) {
        image2x.replace(DIMENSION_REGEX, "1920x1080")
            .replace(VIDEO_EPS_REGEX, "/eps.width=1920,height=1080,quality=100")
            .replace(LANDSCAPE_WITH_LOGO_REGEX, "/landscape-with-logo.width=1920,height=1080,quality=100")
    }

    /**
     * Converts this video into a normalized [PlatformEpisode].
     *
     * @param anime The resolved [PlatformAnime] associated with this episode.
     * @return A normalized [PlatformEpisode] instance.
     */
    fun toPlatformEpisode(anime: PlatformAnime): PlatformEpisode =
        PlatformEpisode(
            anime = anime,
            id = id.toString(),
            title = name,
            description = summary,
            image = image,
            releaseDateTime = releaseDate
        )

    /**
     * Converts this video into the compact projection kept in every verdict. The audio locales
     * are not carried yet: the current model maps one episode per video, so there is no locale
     * to report.
     */
    fun toPlatformItem(): PlatformItem =
        PlatformItem(
            platform = Platform.ANIMATION_DIGITAL_NETWORK,
            platformId = id.toString(),
            showTitle = show.title,
            title = name,
            releaseDateTime = releaseDate.toString(),
            audioLocales = emptyList()
        )
}

/**
 * A single ADN show.
 *
 * @property id Identifier of the show on ADN. A platform page can hold several distinct anime,
 * so this is a platform identifier and not an anime identity.
 * @property title Display title of the show.
 * @property summary Show summary, nullable on the platform.
 * @property genres Genres ADN tags the show with, used to tell anime from live action.
 */
@Serializable
internal data class AdnShow(
    val id: Int,
    val title: String,
    val summary: String?,
    private val image2x: String,
    val genres: List<String>
) {
    /**
     * Portrait 1080x1543 poster URL with logo derived from the 2x source image.
     */
    val thumbnail: String by lazy(LazyThreadSafetyMode.NONE) {
        image2x.replace(DIMENSION_REGEX, "1080x1543")
            .replace(SHOW_PORTRAIT_LOGO_REGEX, "/portrait-with-logo.width=1080,height=1543,quality=100")
    }

    /**
     * Determines whether the show belongs to the animation genre (e.g., Japanese animation).
     * Filters out live-action series and other non-anime programs from the platform catalog.
     *
     * @return `true` if at least one genre contains "Animation ", `false` otherwise.
     */
    val isAnimation: Boolean
        get() = genres.any { it.contains("Animation ") }

    /**
     * Distinguishes an empty genre list from a populated one without the animation genre: both
     * make the show invalid, but they are different data problems worth telling apart.
     *
     * @return The reason to reject this show, or `null` when it is an animation.
     */
    fun rejectionReason(): RejectionReason? =
        when {
            genres.isEmpty() -> RejectionReason.EMPTY_GENRES
            !isAnimation -> RejectionReason.NOT_AN_ANIMATION
            else -> null
        }

    /**
     * Converts this show into a normalized [PlatformAnime].
     *
     * @return A normalized [PlatformAnime] instance.
     */
    fun toPlatformAnime(): PlatformAnime =
        PlatformAnime(
            id = id.toString(),
            title = title,
            description = summary?.replace("\n", ""),
            thumbnail = thumbnail
        )
}
