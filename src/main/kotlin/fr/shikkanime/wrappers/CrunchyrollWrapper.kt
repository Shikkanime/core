package fr.shikkanime.wrappers

import com.google.gson.annotations.SerializedName
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

/**
 * Implementation of the Crunchyroll API
 * Based on https://github.com/crunchy-labs/crunchyroll-rs
 */
object CrunchyrollWrapper {
    enum class SortType {
        NEWLY_ADDED,
        POPULARITY,
        ALPHABETICAL,
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
    )

    data class Season(
        val id: String,
        @SerializedName("subtitle_locales")
        val subtitleLocales: List<String>,
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

    private const val BASE_URL = "https://www.crunchyroll.com/"
    private val seriesRegex = "/series/([A-Z0-9]{9})/".toRegex()
    private val episodeRegex = "/watch/([A-Z0-9]{9})".toRegex()
    private val httpRequest = HttpRequest()
    const val CRUNCHYROLL_CHUNK = 50

    suspend fun getAnonymousAccessToken(): String {
        val response = httpRequest.post(
            "${BASE_URL}auth/v1/token",
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic dC1rZGdwMmg4YzNqdWI4Zm4wZnE6eWZMRGZNZnJZdktYaDRKWFMxTEVJMmNDcXUxdjVXYW4=",
                "ETP-Anonymous-ID" to UUID.randomUUID().toString(),
            ),
            body = "grant_type=client_id&client_id=offline_access"
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get anonymous access token (${response.status})" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsString("access_token")!!
    }

    suspend fun getBrowse(
        locale: String,
        accessToken: String,
        sortBy: SortType = SortType.NEWLY_ADDED,
        type: MediaType = MediaType.EPISODE,
        size: Int = 25,
        start: Int = 0,
        simulcast: String? = null,
    ): Array<BrowseObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/discover/browse?sort_by=${sortBy.name.lowercase()}&type=${type.name.lowercase()}&n=$size&start=$start&locale=$locale${if (simulcast != null) "&seasonal_tag=$simulcast" else ""}",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get media list (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get media list")

        return ObjectParser.fromJson(asJsonArray, Array<BrowseObject>::class.java)
    }

    suspend fun getSeries(locale: String, accessToken: String, id: String): Series {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/series/$id?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get series (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get series")
        return ObjectParser.fromJson(asJsonArray.first(), Series::class.java)
    }

    private suspend fun getSeasonsBySeriesId(locale: String, accessToken: String, seriesId: String): Array<Season> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/series/$seriesId/seasons?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get seasons (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get seasons")
        return ObjectParser.fromJson(asJsonArray, Array<Season>::class.java)

    }

    @JvmStatic
    suspend fun getEpisodesBySeasonId(locale: String, accessToken: String, seasonId: String): Array<Episode> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/seasons/$seasonId/episodes?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get episodes (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get episodes")
        return ObjectParser.fromJson(asJsonArray, Array<Episode>::class.java)
    }

    @JvmStatic
    suspend fun getEpisode(locale: String, accessToken: String, id: String): Episode {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/episodes/$id?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get episode (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get episode")
        return ObjectParser.fromJson(asJsonArray.first(), Episode::class.java)
    }

    private suspend fun getEpisodeByType(locale: String, accessToken: String, id: String, type: String): BrowseObject {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/discover/$type/$id?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get $type episode (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get $type episode")
        return ObjectParser.fromJson(asJsonArray.first().asJsonObject["panel"].asJsonObject, BrowseObject::class.java)
    }

    suspend fun getPreviousEpisode(locale: String, accessToken: String, id: String): BrowseObject {
        return getEpisodeByType(locale, accessToken, id, "previous_episode")
    }

    @JvmStatic
    suspend fun getUpNext(locale: String, accessToken: String, id: String): BrowseObject {
        return getEpisodeByType(locale, accessToken, id, "up_next")
    }

    @JvmStatic
    suspend fun getObjects(locale: String, accessToken: String, vararg ids: String): Array<BrowseObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/objects/${ids.joinToString(",")}?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get objects (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get objects")

        return ObjectParser.fromJson(asJsonArray, Array<BrowseObject>::class.java)
    }

    suspend fun getSimulcasts(locale: String, accessToken: String): List<Simulcast> {
        val response = httpRequest.get(
            "${BASE_URL}content/v1/season_list?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get simulcasts (${response.status})" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")
            ?: throw Exception("Failed to get simulcasts")

        return ObjectParser.fromJson(asJsonArray, Array<Simulcast>::class.java).toList()
    }

    private suspend fun getEpisodesBySeriesId(countryCode: CountryCode, accessToken: String, seriesId: String): List<BrowseObject> {
        val browseObjects = mutableListOf<BrowseObject>()

        val variantObjects = getSeasonsBySeriesId(countryCode.locale, accessToken, seriesId)
            .flatMap { season ->
                getEpisodesBySeasonId(countryCode.locale, accessToken, season.id)
                    .onEach { episode -> browseObjects.add(episode.convertToBrowseObject()) }
                    .flatMap { episode -> episode.versions?.map { it.guid } ?: listOf(episode.id!!) }
            }
            .subtract(browseObjects.map { it.id }.toSet())
            .chunked(CRUNCHYROLL_CHUNK)
            .flatMap { chunk -> HttpRequest.retry(3) { getObjects(countryCode.locale, accessToken, *chunk.toTypedArray()).toList() } }

        return browseObjects + variantObjects
    }

    suspend fun getSimulcastCalendarWithDates(countryCode: CountryCode, accessToken: String, dates: Set<LocalDate>): Array<BrowseObject> {
        val startOfWeekDates = dates.map { it.atStartOfWeek() }.distinct()
        val seriesObjectsCache = MapCache<String, Set<BrowseObject>> { runBlocking { getEpisodesBySeriesId(countryCode, accessToken, it).toSet() } }

        val episodeIds = mutableSetOf<String>()
        val alreadyFetched = mutableMapOf<String, BrowseObject>()

        startOfWeekDates.forEach { date ->
            val response = HttpRequest.retry(3) { httpRequest.get("$BASE_URL${countryCode.name.lowercase()}/simulcastcalendar?filter=premium&date=$date") }
            require(response.status == HttpStatusCode.OK)
            val document = Jsoup.parse(response.bodyAsText())

            document.select("article.release").forEach { element ->
                val isMultipleRelease = element.attr("data-episode-num").contains("-")
                val releaseDateTime = ZonedDateTime.parse(element.select("time").attr("datetime")).withUTC()
                val seriesId = seriesRegex.find(element.select("a").first { seriesRegex.containsMatchIn(it.attr("href")) }.attr("href"))!!.groupValues[1]

                if (isMultipleRelease) {
                    val browseObjects = seriesObjectsCache[seriesId].orEmpty().filter { it.episodeMetadata!!.premiumAvailableDate.withUTC() == releaseDateTime }
                    browseObjects.forEach { alreadyFetched[it.id] = it }
                } else {
                    episodeIds.add(episodeRegex.find(element.select("a").first { episodeRegex.containsMatchIn(it.attr("href")) }.attr("href"))!!.groupValues[1])
                }
            }
        }

        return episodeIds.chunked(CRUNCHYROLL_CHUNK).flatMap { chunk ->
            HttpRequest.retry(3) { getObjects(countryCode.locale, accessToken, *chunk.toTypedArray()).toList() }
        }.toTypedArray() + alreadyFetched.values.toTypedArray()
    }

    fun buildUrl(countryCode: CountryCode, id: String, slugTitle: String?) =
        "${BASE_URL}${countryCode.name.lowercase()}/watch/$id/${slugTitle ?: ""}"
}