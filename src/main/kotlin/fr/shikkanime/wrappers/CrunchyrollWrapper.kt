package fr.shikkanime.wrappers

import com.google.gson.JsonObject
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

    private const val BASE_URL = "https://www.crunchyroll.com/"
    private val httpRequest = HttpRequest()

    suspend fun getAnonymousAccessToken(): String {
        val response = httpRequest.post(
            "${BASE_URL}auth/v1/token",
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic Y3Jfd2ViOg==",
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

    suspend fun getSeasonsBySeriesId(locale: String, accessToken: String, seriesId: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/series/$seriesId/seasons?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get seasons" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get seasons")
    }

    suspend fun getEpisodesBySeasonId(locale: String, accessToken: String, seasonId: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BASE_URL}content/v2/cms/seasons/$seasonId/episodes?locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get episodes" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get episodes")
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