package fr.shikkanime.wrappers.factories

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

abstract class AbstractCrunchyrollWrapper {
    enum class SortType {
        NEWLY_ADDED,
        POPULARITY,
    }

    enum class MediaType {
        EPISODE,
        SERIES,
    }

    data class MediaImage(
        val source: String,
        val type: String,
        val width: Int,
        val height: Int,
    ) : Serializable

    data class Image(
        val thumbnail: List<List<MediaImage>> = emptyList(),
        @SerializedName("poster_tall")
        val posterTall: List<List<MediaImage>> = emptyList(),
        @SerializedName("poster_wide")
        val posterWide: List<List<MediaImage>> = emptyList(),
    ) : Serializable {
        val fullHDThumbnail: String?
            get() = thumbnail.firstOrNull()?.maxByOrNull { it.width }?.source
        val fullHDImage: String?
            get() = posterTall.firstOrNull()?.maxByOrNull { it.width }?.source
        val fullHDBanner: String?
            get() = posterWide.firstOrNull()?.maxByOrNull { it.width }?.source
    }

    data class Version(
        val guid: String,
        @SerializedName("audio_locale")
        val audioLocale: String,
        val original: Boolean
    ) : Serializable

    data class Series(
        val id: String,
        val images: Image,
        val title: String,
        @SerializedName("slug_title")
        val slugTitle: String,
        val description: String?,
        @SerializedName("is_simulcast")
        val isSimulcast: Boolean,
    ) : Serializable {
        fun convertToBrowseObject() = BrowseObject(
            id = id,
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
        @SerializedName("subtitle_locales")
        val subtitleLocales: Set<String>,
        val keywords: Set<String>
    ) : Serializable

    data class Episode(
        val id: String,
        @SerializedName("series_id")
        val seriesId: String,
        @SerializedName("series_title")
        val seriesTitle: String,
        @SerializedName("series_slug_title")
        val seriesSlugTitle: String?,
        @SerializedName("audio_locale")
        val audioLocale: String,
        @SerializedName("subtitle_locales")
        val subtitleLocales: List<String>,
        @SerializedName("premium_available_date")
        val premiumAvailableDate: ZonedDateTime,
        @SerializedName("season_id")
        val seasonId: String,
        @SerializedName("season_number")
        val seasonNumber: Int?,
        @SerializedName("season_slug_title")
        val seasonSlugTitle: String?,
        @SerializedName("episode")
        val numberString: String,
        @SerializedName("episode_number")
        val number: Int?,
        val title: String?,
        @SerializedName("slug_title")
        val slugTitle: String?,
        val images: Image?,
        @SerializedName("duration_ms")
        val durationMs: Long,
        val description: String?,
        @SerializedName("mature_blocked")
        val matureBlocked: Boolean,
        val versions: List<Version>?,
        @SerializedName("next_episode_id")
        val nextEpisodeId: String?,
        @SerializedName("season_sequence_number")
        val seasonSequenceNumber: Int,
        @SerializedName("sequence_number")
        val sequenceNumber: Double,
        @SerializedName("identifier")
        val identifier: String?,
    ) : Serializable {
        fun index() = ((seasonSequenceNumber - 1) * 100) + sequenceNumber

        fun convertToBrowseObject() = BrowseObject(
            id = id,
            images = images,
            description = description,
            title = title,
            seriesMetadata = null,
            episodeMetadata = this,
            slugTitle = slugTitle,
        )

        fun getVariants(original: Boolean? = null): List<String> {
            if (versions.isNullOrEmpty())
                return listOf(id)

            if (original == null)
                return versions.map { it.guid }

            return versions.filter { it.original == original }.map { it.guid }
        }
    }

    data class BrowseObject(
        val id: String,
        val images: Image?,
        val description: String?,
        val title: String?,
        @SerializedName("series_metadata")
        val seriesMetadata: Series?,
        @SerializedName("episode_metadata")
        val episodeMetadata: Episode?,
        @SerializedName("slug_title")
        val slugTitle: String?,
    ) : Serializable {
        val fullHDCarousel: String
            get() = "https://imgsrv.crunchyroll.com/cdn-cgi/image/format=png,quality=100,width=1920/keyart/$id-backdrop_wide"
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
                    HttpHeaders.ContentType to "application/x-www-form-urlencoded",
                    HttpHeaders.Authorization to "Basic bjBxMm54bDM3emlnMGRxbjBhaW86UDBqM2hlNE44VUc4VzJ3MC1QdkRhZGpHdXY2MmZOMmQ=",
                    "ETP-Anonymous-ID" to UUID.randomUUID().toString(),
                ),
                body = "grant_type=client_id&client_id=offline_access"
            )
            require(response.status == HttpStatusCode.OK) { "Failed to get anonymous access token (${response.status.value})" }
            ObjectParser.fromJson(response.bodyAsText()).getAsString("access_token")!!
        }
    }

    protected suspend fun HttpRequest.getWithAccessToken(url: String) = get(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAnonymousAccessToken()}"))

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
            ?.sortedWith(compareBy({ it.episodeMetadata!!.seasonSequenceNumber }, { it.episodeMetadata!!.sequenceNumber }))
            ?.lastOrNull { it.episodeMetadata!!.index() < episode.index() }
            ?.let { return it }

        return null
    }

    suspend fun retrieveNextEpisode(locale: String, id: String): BrowseObject? {
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
            ?.sortedWith(compareBy({ it.episodeMetadata!!.seasonSequenceNumber }, { it.episodeMetadata!!.sequenceNumber }))
            ?.firstOrNull { it.episodeMetadata!!.index() > episode.index() }
            ?.let { return it }

        return null
    }

    fun buildUrl(countryCode: CountryCode, id: String, slugTitle: String?) =
        "${baseUrl}${countryCode.name.lowercase()}/watch/$id/${slugTitle ?: StringUtils.EMPTY_STRING}"

    companion object {
        const val CRUNCHYROLL_CHUNK = 100
    }
}