package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.ObjectParser.getNullableJsonObject
import io.ktor.client.statement.*

/**
 * Implementation of the Crunchyroll API
 * Based on https://github.com/crunchy-labs/crunchyroll-rs
 */
object CrunchyrollWrapper {
    data class CMS(
        val bucket: String,
        val policy: String,
        val signature: String,
        val keyPairId: String,
        val expires: String,
    )

    enum class SortType {
        NEWLY_ADDED,
        POPULARITY,
        ALPHABETICAL,
    }

    enum class MediaType {
        EPISODE,
        SERIES,
    }

    private const val BETA_URL = "https://beta-api.crunchyroll.com/"
    private const val BASE_URL = "https://www.crunchyroll.com/"
    private val httpRequest = HttpRequest()

    suspend fun getAnonymousAccessToken(): String {
        val response = httpRequest.post(
            "${BASE_URL}auth/v1/token",
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic b2VkYXJteHN0bGgxanZhd2ltbnE6OWxFaHZIWkpEMzJqdVY1ZFc5Vk9TNTdkb3BkSnBnbzE="
            ),
            body = "grant_type=client_id&client_id=offline_access"
        )

        require(response.status.value == 200) { "Failed to get anonymous access token" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsString("access_token")!!
    }

    suspend fun getCMS(accessToken: String): CMS {
        val response = httpRequest.get(
            "${BASE_URL}index/v2",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get CMS" }

        return ObjectParser.fromJson(response.bodyAsText()).getNullableJsonObject("cms")?.let {
            CMS(
                bucket = it.getAsString("bucket")!!,
                policy = it.getAsString("policy")!!,
                signature = it.getAsString("signature")!!,
                keyPairId = it.getAsString("key_pair_id")!!,
                expires = it.getAsString("expires")!!,
            )
        } ?: throw Exception("Failed to get CMS")
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

    suspend fun getObject(locale: String, accessToken: String, cms: CMS, vararg ids: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BETA_URL}cms/v2${cms.bucket}/objects/${ids.joinToString(",")}?Policy=${cms.policy}&Signature=${cms.signature}&Key-Pair-Id=${cms.keyPairId}&locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get media object" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get media object")
    }

    suspend fun getSeasons(locale: String, accessToken: String, cms: CMS, seriesId: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BETA_URL}cms/v2${cms.bucket}/seasons?series_id=$seriesId&Policy=${cms.policy}&Signature=${cms.signature}&Key-Pair-Id=${cms.keyPairId}&locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get seasons" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get seasons")
    }

    suspend fun getEpisodes(locale: String, accessToken: String, cms: CMS, seasonId: String): List<JsonObject> {
        val response = httpRequest.get(
            "${BETA_URL}cms/v2${cms.bucket}/episodes?season_id=$seasonId&Policy=${cms.policy}&Signature=${cms.signature}&Key-Pair-Id=${cms.keyPairId}&locale=$locale",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        require(response.status.value == 200) { "Failed to get episodes" }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")?.map { it.asJsonObject }
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
        "https://www.crunchyroll.com/${countryCode.name.lowercase()}/watch/$id/${slugTitle ?: ""}"
}