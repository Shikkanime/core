package fr.shikkanime.wrappers.factories

import com.google.gson.annotations.SerializedName
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
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
    )

    data class Image(
        val thumbnail: List<List<MediaImage>> = emptyList(),
        @SerializedName("poster_tall")
        val posterTall: List<List<MediaImage>> = emptyList(),
        @SerializedName("poster_wide")
        val posterWide: List<List<MediaImage>> = emptyList(),
    )

    data class Version(
        val guid: String,
        val original: Boolean,
    )

    data class Series(
        val id: String,
        val images: Image,
        val title: String,
        val description: String?,
        @SerializedName("is_simulcast")
        val isSimulcast: Boolean,
    ) {
        val fullHDImage: String?
            get() = images.posterTall.firstOrNull()?.maxByOrNull { it.width }?.source
        val fullHDBanner: String?
            get() = images.posterWide.firstOrNull()?.maxByOrNull { it.width }?.source
    }

    data class Season(
        val id: String,
        @SerializedName("subtitle_locales")
        val subtitleLocales: List<String>,
        val keywords: List<String>,
    )

    data class Episode(
        val id: String?,
        @SerializedName("series_id")
        val seriesId: String,
        @SerializedName("series_title")
        val seriesTitle: String,
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
    ) {
        fun convertToBrowseObject() = BrowseObject(
            id = id!!,
            images = images,
            description = description,
            title = title,
            episodeMetadata = this,
            slugTitle = slugTitle,
        )

        fun getVariants(original: Boolean? = null): List<String> {
            if (versions.isNullOrEmpty())
                return listOf(id!!)

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
        @SerializedName("episode_metadata")
        val episodeMetadata: Episode?,
        @SerializedName("slug_title")
        val slugTitle: String?,
    )

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
        key = ""
    ) {
        runBlocking {
            val response = httpRequest.post(
                "${baseUrl}auth/v1/token",
                headers = mapOf(
                    HttpHeaders.ContentType to "application/x-www-form-urlencoded",
                    HttpHeaders.Authorization to "Basic dC1rZGdwMmg4YzNqdWI4Zm4wZnE6eWZMRGZNZnJZdktYaDRKWFMxTEVJMmNDcXUxdjVXYW4=",
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
    abstract suspend fun getSeasonsBySeriesId(locale: String, id: String): List<Season>
    abstract suspend fun getSeason(locale: String, id: String): Season
    abstract suspend fun getEpisodesBySeasonId(locale: String, id: String): List<Episode>
    abstract suspend fun getEpisode(locale: String, id: String): Episode
    abstract suspend fun getEpisodeByType(locale: String, type: String, id: String): BrowseObject
    abstract suspend fun getObjects(locale: String, vararg ids: String): List<BrowseObject>
    abstract suspend fun getEpisodesBySeriesId(locale: String, id: String, original: Boolean? = null): List<BrowseObject>

    fun buildUrl(countryCode: CountryCode, id: String, slugTitle: String?) =
        "${baseUrl}${countryCode.name.lowercase()}/watch/$id/${slugTitle ?: ""}"

    companion object {
        const val CRUNCHYROLL_CHUNK = 100
    }
}