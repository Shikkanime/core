package fr.shikkanime.wrappers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.ObjectParser.getNullableJsonObject
import io.ktor.client.statement.*

object CrunchyrollWrapper {
    data class CMS(
        val bucket: String,
        val policy: String,
        val signature: String,
        val keyPairId: String,
        val expires: String,
    )

    private const val BASE_URL = "https://beta-api.crunchyroll.com/"
    private const val LOCALE = "fr-FR"

    suspend fun getAnonymousAccessToken(): String {
        val httpRequest = HttpRequest()

        val response = httpRequest.post(
            "${BASE_URL}auth/v1/token",
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic b2VkYXJteHN0bGgxanZhd2ltbnE6OWxFaHZIWkpEMzJqdVY1ZFc5Vk9TNTdkb3BkSnBnbzE="
            ),
            body = "grant_type=client_id&client_id=offline_access"
        )

        if (response.status.value != 200) {
            throw Exception("Failed to get anonymous access token")
        }

        return ObjectParser.fromJson(response.bodyAsText()).getAsString("access_token")!!
    }

    suspend fun getCMS(accessToken: String): CMS {
        val httpRequest = HttpRequest()

        val response = httpRequest.get(
            "${BASE_URL}index/v2",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        if (response.status.value != 200) {
            throw Exception("Failed to get anonymous access token")
        }

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

    suspend fun getNewlyAdded(accessToken: String, size: Int = 25, type: String = "episode"): List<JsonObject> {
        val httpRequest = HttpRequest()

        val response = httpRequest.get(
            "${BASE_URL}content/v1/browse?sort_by=newly_added&n=$size&start=0&locale=$LOCALE&type=$type",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        if (response.status.value != 200) {
            throw Exception("Failed to get media list")
        }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get media list")
    }

    suspend fun getObject(accessToken: String, cms: CMS, vararg ids: String): JsonArray {
        val httpRequest = HttpRequest()

        val response = httpRequest.get(
            "${BASE_URL}cms/v2${cms.bucket}/objects/${ids.joinToString(",")}?Policy=${cms.policy}&Signature=${cms.signature}&Key-Pair-Id=${cms.keyPairId}&locale=$LOCALE",
            headers = mapOf(
                "Authorization" to "Bearer $accessToken",
            ),
        )

        if (response.status.value != 200) {
            throw Exception("Failed to get media object")
        }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("items")
            ?: throw Exception("Failed to get media object")
    }
}