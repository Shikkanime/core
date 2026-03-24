package fr.shikkanime.wrappers.factories

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.serializers.ZonedDateTimeSerializer
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import java.io.Serializable
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

abstract class AbstractCrunchyrollWrapper {
    @kotlinx.serialization.Serializable
    data class CrunchyrollResponse<T>(
        val data: List<T>
    )

    @kotlinx.serialization.Serializable
    data class Playhead(
        val panel: BrowseObject
    )

    enum class SortType {
        NEWLY_ADDED,
        POPULARITY,
    }

    enum class MediaType {
        EPISODE,
        SERIES,
    }

    @kotlinx.serialization.Serializable
    data class MediaImage(
        val source: String,
        val type: String,
        val width: Int,
        val height: Int,
    ) : Serializable

    @kotlinx.serialization.Serializable
    data class Image(
        val thumbnail: List<List<MediaImage>> = emptyList(),
        @SerializedName("poster_tall")
        @SerialName("poster_tall")
        val posterTall: List<List<MediaImage>> = emptyList(),
        @SerializedName("poster_wide")
        @SerialName("poster_wide")
        val posterWide: List<List<MediaImage>> = emptyList(),
    ) : Serializable {
        val fullHDThumbnail: String?
            get() = thumbnail.firstOrNull()?.maxByOrNull { it.width }?.source
        val fullHDImage: String?
            get() = posterTall.firstOrNull()?.maxByOrNull { it.width }?.source
        val fullHDBanner: String?
            get() = posterWide.firstOrNull()?.maxByOrNull { it.width }?.source
    }

    @kotlinx.serialization.Serializable
    data class Version(
        val guid: String,
        @SerializedName("audio_locale")
        @SerialName("audio_locale")
        val audioLocale: String,
        val original: Boolean
    ) : Serializable

    @kotlinx.serialization.Serializable
    data class Series(
        val id: String? = null,
        val images: Image? = null,
        val title: String? = null,
        @SerializedName("slug_title")
        @SerialName("slug_title")
        val slugTitle: String? = null,
        val description: String? = null,
        @SerializedName("is_simulcast")
        @SerialName("is_simulcast")
        val isSimulcast: Boolean,
    ) : Serializable {
        fun convertToBrowseObject() = BrowseObject(
            id = requireNotNull(id) { "Series ID cannot be null to convert to BrowseObject" },
            images = images,
            description = description,
            title = title,
            seriesMetadata = this,
            episodeMetadata = null,
            slugTitle = slugTitle,
        )
    }

    data class Season(
        val id: String,
        val description: String?,
        @SerializedName("subtitle_locales")
        val subtitleLocales: Set<String>,
        val keywords: Set<String>
    ) : Serializable

    @kotlinx.serialization.Serializable
    data class Episode(
        val id: String? = null,
        @SerializedName("series_id")
        @SerialName("series_id")
        val seriesId: String,
        @SerializedName("series_title")
        @SerialName("series_title")
        val seriesTitle: String,
        @SerializedName("series_slug_title")
        @SerialName("series_slug_title")
        val seriesSlugTitle: String?,
        @SerializedName("audio_locale")
        @SerialName("audio_locale")
        val audioLocale: String,
        @SerializedName("subtitle_locales")
        @SerialName("subtitle_locales")
        val subtitleLocales: List<String>,
        @kotlinx.serialization.Serializable(with = ZonedDateTimeSerializer::class)
        @SerializedName("premium_available_date")
        @SerialName("premium_available_date")
        val premiumAvailableDate: ZonedDateTime,
        @SerializedName("season_id")
        @SerialName("season_id")
        val seasonId: String,
        @SerializedName("season_number")
        @SerialName("season_number")
        val seasonNumber: Int?,
        @SerializedName("season_slug_title")
        @SerialName("season_slug_title")
        val seasonSlugTitle: String?,
        @SerializedName("episode")
        @SerialName("episode")
        val numberString: String,
        @SerializedName("episode_number")
        @SerialName("episode_number")
        val number: Int?,
        val title: String? = null,
        @SerializedName("slug_title")
        @SerialName("slug_title")
        val slugTitle: String? = null,
        val images: Image? = null,
        @SerializedName("duration_ms")
        @SerialName("duration_ms")
        val durationMs: Long,
        val description: String? = null,
        @SerializedName("mature_blocked")
        @SerialName("mature_blocked")
        val matureBlocked: Boolean,
        val versions: List<Version>?,
        @SerializedName("next_episode_id")
        @SerialName("next_episode_id")
        val nextEpisodeId: String? = null,
        @SerializedName("season_sequence_number")
        @SerialName("season_sequence_number")
        val seasonSequenceNumber: Int,
        @SerializedName("sequence_number")
        @SerialName("sequence_number")
        val sequenceNumber: Double,
        @SerializedName("identifier")
        @SerialName("identifier")
        val identifier: String?,
    ) : Serializable {
        fun index() = ((seasonSequenceNumber - 1) * 100) + sequenceNumber

        fun convertToBrowseObject() = BrowseObject(
            id = requireNotNull(id) { "Episode ID cannot be null to convert to BrowseObject" },
            images = images,
            description = description,
            title = title,
            seriesMetadata = null,
            episodeMetadata = this,
            slugTitle = slugTitle,
        )

        fun getVariants(original: Boolean? = null): List<String> {
            if (versions.isNullOrEmpty())
                return listOfNotNull(id)

            if (original == null)
                return versions.map { it.guid }

            return versions.filter { it.original == original }.map { it.guid }
        }
    }

    @kotlinx.serialization.Serializable
    data class BrowseObject(
        val id: String,
        val images: Image?,
        val description: String?,
        val title: String?,
        @SerializedName("series_metadata")
        @SerialName("series_metadata")
        val seriesMetadata: Series? = null,
        @SerializedName("episode_metadata")
        @SerialName("episode_metadata")
        val episodeMetadata: Episode? = null,
        @SerializedName("slug_title")
        @SerialName("slug_title")
        val slugTitle: String?,
    ) : Serializable {
        val fullHDCarousel: String
            get() = "https://imgsrv.crunchyroll.com/cdn-cgi/image/format=png,quality=100,width=1920/keyart/$id-backdrop_wide"
        val fullHDTitle: String
            get() = "https://imgsrv.crunchyroll.com/cdn-cgi/image/format=png,quality=100,width=1920/keyart/$id-title_logo-en-us"

        fun getNormalizedDescription(): String? {
            return description?.split("\r\n\r\n")?.let { lines ->
                when {
                    lines.size == 2 -> lines[1]
                    lines.size > 2 -> lines.subList(1, lines.size - 1).joinToString(StringUtils.SPACE_STRING)
                    else -> lines.firstOrNull()
                }
            }
        }
    }

    data class Localization(
        val title: String,
        val description: String,
    )

    data class Simulcast(
        val id: String,
        val localization: Localization,
    )

    protected val baseUrl = "https://www.crunchyroll.com/"
    protected val httpRequest = HttpRequest()
    private val configCacheService by lazy { Constant.injector.getInstance(ConfigCacheService::class.java) }

    @Synchronized
    private fun getAnonymousAccessToken() = MapCache.getOrCompute(
        "AbstractCrunchyrollWrapper.getAnonymousAccessToken",
        duration = Duration.ofMinutes(30),
        typeToken = object : TypeToken<MapCacheValue<String>>() {},
        key = StringUtils.EMPTY_STRING
    ) {
        runBlocking {
            val response = httpRequest.post(
                "${baseUrl}auth/v1/token",
                headers = mapOf(
                    HttpHeaders.ContentType to ContentType.Application.FormUrlEncoded.toString(),
                    HttpHeaders.Authorization to "Basic ${configCacheService.getValueAsString(ConfigPropertyKey.CRUNCHYROLL_BASIC_AUTH_TOKEN, CRUNCHYROLL_BASIC_AUTH_TOKEN_DEFAULT)}",
                    "ETP-Anonymous-ID" to UUID.randomUUID().toString(),
                ),
                body = "grant_type=client_id&client_id=offline_access"
            )
            require(response.status == HttpStatusCode.OK) { "Failed to get anonymous access token (${response.status.value})" }
            ObjectParser.fromJson(response.bodyAsText()).getAsString("access_token")!!
        }
    }

    protected suspend fun HttpRequest.getWithAccessToken(url: String) = get(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAnonymousAccessToken()}"))

    protected suspend fun getEpisodesBySeriesIdBase(
        locale: String,
        id: String,
        original: Boolean? = null
    ): Array<BrowseObject> {
        val allEpisodes = getSeasonsBySeriesId(locale, id)
            .flatMap { season -> getEpisodesBySeasonId(locale, season.id).toList() }

        val mainBrowseObjects = allEpisodes.map { it.convertToBrowseObject() }
        val mainIds = mainBrowseObjects.map { it.id }.toSet()

        val variantIds = allEpisodes
            .flatMap { it.getVariants(original) }
            .filter { it !in mainIds }

        val variantBrowseObjects = getChunkedObjects(locale, *variantIds.toTypedArray())

        return (mainBrowseObjects + variantBrowseObjects).toTypedArray()
    }

    abstract suspend fun getBrowse(locale: String, sortBy: SortType = SortType.NEWLY_ADDED, type: MediaType = MediaType.EPISODE, size: Int = 25, start: Int = 0, simulcast: String? = null): List<BrowseObject>
    abstract suspend fun getSeries(locale: String, id: String): Series
    abstract suspend fun getSeasonsBySeriesId(locale: String, id: String): Array<Season>
    abstract suspend fun getSeason(locale: String, id: String): Season
    abstract suspend fun getEpisodesBySeasonId(locale: String, id: String): Array<Episode>
    abstract suspend fun getEpisode(locale: String, id: String): Episode
    abstract suspend fun getEpisodeDiscoverByType(locale: String, type: String, id: String): BrowseObject
    abstract suspend fun getObjects(locale: String, vararg ids: String): List<BrowseObject>
    suspend fun getChunkedObjects(locale: String, vararg ids: String): List<BrowseObject> = ids.toSet().chunked(CRUNCHYROLL_CHUNK).flatMap { getObjects(locale, *it.toTypedArray()) }
    abstract suspend fun getEpisodesBySeriesId(locale: String, id: String, original: Boolean? = null): Array<BrowseObject>
    abstract suspend fun getSimulcasts(locale: String): Array<Simulcast>

    suspend fun retrievePreviousEpisode(locale: String, id: String): BrowseObject? {
        // Attempt to fetch the previous episode directly
        runCatching { getEpisodeDiscoverByType(locale, "previous_episode", id) }
            .getOrNull()
            ?.let { return it }

        val episode = runCatching { getEpisode(locale, id) }.getOrNull() ?: return null

        // Fetch episodes by season and find the previous episode
        runCatching { getEpisodesBySeasonId(locale, episode.seasonId) }
            .getOrNull()
            ?.sortedBy { it.sequenceNumber }
            ?.firstOrNull { it.sequenceNumber < episode.sequenceNumber }
            ?.let { return it.convertToBrowseObject() }

        // Fetch episodes by series and find the previous episode
        runCatching { getEpisodesBySeriesId(locale, episode.seriesId) }
            .getOrNull()
            ?.sortedWith(
                compareBy(
                    { it.episodeMetadata!!.seasonSequenceNumber },
                    { it.episodeMetadata!!.sequenceNumber })
            )
            ?.lastOrNull { it.episodeMetadata!!.index() < episode.index() }
            ?.let { return it }

        return null
    }

    suspend fun retrieveNextEpisode(locale: String, id: String): BrowseObject? {
        // Attempt to fetch the next episode directly
        runCatching { getEpisodeDiscoverByType(locale, "up_next", id) }
            .getOrNull()
            ?.let { return it }

        // Fetch the current episode and check for nextEpisodeId
        val episode = runCatching { getEpisode(locale, id) }.getOrNull() ?: return null
        episode.nextEpisodeId?.let { return getObjects(locale, it).firstOrNull() }

        // Fetch episodes by season and find the next episode
        runCatching { getEpisodesBySeasonId(locale, episode.seasonId) }
            .getOrNull()
            ?.sortedBy { it.sequenceNumber }
            ?.firstOrNull { it.sequenceNumber > episode.sequenceNumber }
            ?.let { return it.convertToBrowseObject() }

        // Fetch episodes by series and find the next episode
        runCatching { getEpisodesBySeriesId(locale, episode.seriesId) }
            .getOrNull()
            ?.sortedWith(
                compareBy(
                    { it.episodeMetadata!!.seasonSequenceNumber },
                    { it.episodeMetadata!!.sequenceNumber })
            )
            ?.firstOrNull { it.episodeMetadata!!.index() > episode.index() }
            ?.let { return it }

        return null
    }

    fun buildUrl(countryCode: CountryCode, id: String, slugTitle: String?) =
        "${baseUrl}${countryCode.name.lowercase()}/watch/$id/${slugTitle ?: StringUtils.EMPTY_STRING}"

    companion object {
        const val CRUNCHYROLL_CHUNK = 100
        // Updated automatically by the update-credentials GitHub Actions workflow - do not edit manually
        const val CRUNCHYROLL_BASIC_AUTH_TOKEN_DEFAULT = "aG85ZGNjdjI5NGRueHV4YzFnaXI6OC11MDdwOGVpYmh1bU1pNEhabTV1ano0YWY5S3VseTU="
        const val CRUNCHYROLL_APK_VERSION = "0.0.0_0"
    }
}