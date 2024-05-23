package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
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
        val thumbnail: List<List<MediaImage>>,
    )

    data class Version(
        val guid: String,
    )

    data class Season(
        val id: String,
        @SerializedName("subtitle_locales")
        val subtitleLocales: List<String>,
    )

    data class Episode(
        val id: String,
        @SerializedName("eligible_region")
        val eligibleRegion: String,
        @SerializedName("audio_locale")
        val audioLocale: String,
        @SerializedName("subtitle_locales")
        val subtitleLocales: List<String>,
        @SerializedName("premium_available_date")
        val premiumAvailableDate: String,
        @SerializedName("season_number")
        val seasonNumber: Int?,
        @SerializedName("season_slug_title")
        val seasonSlugTitle: String?,
        @SerializedName("episode")
        val numberString: String,
        @SerializedName("episode_number")
        val number: Int?,
        @SerializedName("title")
        val title: String?,
        @SerializedName("slug_title")
        val slugTitle: String?,
        val images: Image?,
        @SerializedName("duration_ms")
        val durationMs: Long,
        @SerializedName("description")
        val description: String?,
        val versions: List<Version>?,
    )

    private const val BASE_URL = "https://www.crunchyroll.com/"
    private val httpRequest = HttpRequest()

    suspend fun getAnonymousAccessToken(): String {
        val response = httpRequest.post(
            "${BASE_URL}auth/v1/token",
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic YWNmYWZtNTE3aGtpZWt4Yl93bWU6MDluclZfejBUNWxVdjRyRHp5ZlJYZk0wVmlIRHQyQV8=",
                "ETP-Anonymous-ID" to UUID.randomUUID().toString(),
            ),
            body = "grant_type=client_id&client_id=offline_access"
        )

        require(response.status.value == 200) { "Failed to get anonymous access token" }

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
    ): List<JsonObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/discover/browse?sort_by=${sortBy.name.lowercase()}&type=${type.name.lowercase()}&n=$size&start=$start&locale=$locale${if (simulcast != null) "&seasonal_tag=$simulcast" else ""}",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get media list (${response.status.value})" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get media list")
    }

    suspend fun getSeries(locale: String, accessToken: String, vararg ids: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/series/${ids.joinToString(",")}?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get series" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get series")
    }

    suspend fun getSeasonsBySeriesId(locale: String, accessToken: String, seriesId: String): Array<Season> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/series/$seriesId/seasons?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get seasons" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get seasons")
        return ObjectParser.fromJson(asJsonArray, Array<Season>::class.java)

    }

    suspend fun getEpisodesBySeasonId(locale: String, accessToken: String, seasonId: String): Array<Episode> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/seasons/$seasonId/episodes?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get episodes" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get episodes")
        return ObjectParser.fromJson(asJsonArray, Array<Episode>::class.java)
    }

    suspend fun getEpisode(locale: String, accessToken: String, id: String): Episode {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/episodes/$id?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get episode" }

        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")
            ?: throw Exception("Failed to get episode")
        return ObjectParser.fromJson(asJsonArray.first(), Episode::class.java)
    }

    suspend fun getSimulcasts(locale: String, accessToken: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v1/season_list?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get simulcasts" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get simulcasts")
    }

    fun buildUrl(countryCode: CountryCode, id: String, slugTitle: String?) =
        "${BASE_URL}${countryCode.name.lowercase()}/watch/$id/${slugTitle ?: ""}"
}