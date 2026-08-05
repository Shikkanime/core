package fr.shikkanime.jobs.platforms.impl

import fr.shikkanime.jobs.SmartHttpClient
import fr.shikkanime.jobs.ZonedDateTimeSerializer
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.StreamingPlatform
import fr.shikkanime.models.Platform
import io.ktor.http.HttpHeaders
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import java.time.ZonedDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
            .minus(1, DateTimeUnit.DAY)
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

            cachedEpisodes = response.data.videos.mapNotNull { video ->
                if (video.show.genres.isEmpty() || video.show.genres.none { it.contains("Animation ") }) return@mapNotNull null

                PlatformEpisode(
                    anime = animes.getOrPut(video.show.id) {
                        PlatformAnime(
                            id = video.show.id.toString(),
                            title = video.show.title,
                            description = video.show.summary,
                            thumbnail = video.show.thumbnail,
                        )
                    },
                    id = video.id.toString(),
                    title = video.name,
                    description = video.summary,
                    image = video.image,
                    releaseDateTime = video.releaseDate
                )
            }
        }

        return cachedEpisodes
    }
}

@Serializable
internal data class AdnCalendarResponse(
    val videos: List<AdnVideo>
)

@Serializable
internal data class AdnVideo(
    val id: Int,
    val name: String,
    private val image2x: String,
    val summary: String?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val releaseDate: ZonedDateTime,
    val show: AdnShow
) {
    val image: String
        get() = image2x.replace("\\d+x\\d+".toRegex(), "1920x1080")
            .replace("/eps$".toRegex(), "/eps.width=1920,height=1080,quality=100")
}

@Serializable
internal data class AdnShow(
    val id: Int,
    val title: String,
    val summary: String?,
    private val image2x: String,
    val genres: List<String>
) {
    val thumbnail: String
        get() = image2x.replace("\\d+x\\d+".toRegex(), "1080x1543")
            .replace("/portrait-with-logo$".toRegex(), "/portrait-with-logo.width=1080,height=1543,quality=100")
}