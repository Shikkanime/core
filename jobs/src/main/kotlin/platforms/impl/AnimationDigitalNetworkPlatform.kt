package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.ZonedDateTimeSerializer
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.models.Platform
import io.ktor.http.*
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
private val LANDSCAPE_WITH_LOGO_REGEX = Regex("""/landscape-with-logo$""")
private val SHOW_PORTRAIT_LOGO_REGEX = Regex("""/portrait-with-logo$""")
private val TRAILER_INDICATORS = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
private val SPECIAL_SHOW_TYPES = setOf(AdnVideoType.PV, AdnVideoType.BONUS)

@Single(binds = [StreamingPlatform::class])
class AnimationDigitalNetworkPlatform(
    private val client: SmartHttpClient,
    private val ttl: Duration = 1.minutes
) : StreamingPlatform {
    override val platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK
    private var cachedEpisodes: List<PlatformEpisode> = emptyList()

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

            cachedEpisodes = response.data.videos.mapNotNull { video ->
                if (!video.isValid) return@mapNotNull null
                val anime = resolveAnime(video.show, animes, invalidShowIds) ?: return@mapNotNull null
                video.toPlatformEpisode(anime)
            }
        }

        return cachedEpisodes
    }

    /**
     * Resolves the [PlatformAnime] for a given [show], using the local cache [animes] to avoid duplicate
     * conversions and tracking excluded non-animation shows in [invalidShowIds].
     *
     * @param show The raw ADN show metadata.
     * @param animes Cache mapping valid ADN show IDs to their converted [PlatformAnime].
     * @param invalidShowIds Set of show IDs known to not match animation genre criteria.
     * @return The converted [PlatformAnime], or `null` if the show is not an animation.
     */
    private fun resolveAnime(
        show: AdnShow,
        animes: MutableMap<Int, PlatformAnime>,
        invalidShowIds: MutableSet<Int>
    ): PlatformAnime? {
        if (show.id in invalidShowIds) return null

        return animes[show.id] ?: run {
            if (!show.isAnimation) {
                invalidShowIds.add(show.id)
                return null
            }

            show.toPlatformAnime()
                .also { animes[show.id] = it }
        }
    }
}

@Serializable
internal data class AdnCalendarResponse(
    val videos: List<AdnVideo>
)

internal enum class AdnVideoType {
    EPS,
    PV,
    BONUS
}

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
     * Determines whether this video is a standard broadcast episode.
     * Excludes promotional videos (PV), bonus content, and trailers/openings/making-of clips.
     *
     * @return `true` if the video is a regular broadcast episode, `false` otherwise.
     */
    val isValid: Boolean
        get() = type !in SPECIAL_SHOW_TYPES
                && TRAILER_INDICATORS.none { shortNumber.startsWith(it) }

    /**
     * Converts this platform-specific video representation into a normalized [PlatformEpisode].
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
}

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
     * Converts this platform-specific show representation into a normalized [PlatformAnime].
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
