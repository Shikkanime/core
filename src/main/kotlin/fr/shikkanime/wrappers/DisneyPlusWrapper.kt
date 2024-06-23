package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*

object DisneyPlusWrapper {
    suspend fun getAccessToken(authorization: String?, refreshToken: String?): String {
        require(!authorization.isNullOrBlank() && !refreshToken.isNullOrBlank()) { "Missing Disney+ authorization or refresh token" }

        val loginDevice = HttpRequest().post(
            "https://disney.api.edge.bamgrid.com/graph/v1/device/graphql",
            headers = mapOf(
                "Authorization" to authorization,
            ),
            body = ObjectParser.toJson(
                mapOf(
                    "operationName" to "refreshToken",
                    "query" to "mutation refreshToken(\$input:RefreshTokenInput!){refreshToken(refreshToken:\$input){activeSession{sessionId}}}",
                    "variables" to mapOf(
                        "input" to mapOf(
                            "refreshToken" to refreshToken
                        )
                    ),
                )
            )
        )

        require(loginDevice.status.value == 200) { "Failed to login to Disney+" }
        val loginDeviceJson = ObjectParser.fromJson(loginDevice.bodyAsText(), JsonObject::class.java)
        return loginDeviceJson.getAsJsonObject("extensions")
            .getAsJsonObject("sdk")
            .getAsJsonObject("token")
            .getAsString("accessToken")!!
    }

    suspend fun getAnimeDetailsWithSeasons(accessToken: String, id: String): Pair<JsonObject, List<String>> {
        val seasonsResponse = HttpRequest().get(
            "https://disney.api.edge.bamgrid.com/explore/v1.4/page/entity-$id?disableSmartFocus=true&enhancedContainersLimit=15&limit=15",
            mapOf("Authorization" to "Bearer $accessToken")
        )

        require(seasonsResponse.status.value == 200) { "Failed to fetch Disney+ content" }
        val jsonObject = ObjectParser.fromJson(seasonsResponse.bodyAsText(), JsonObject::class.java)
        val pageObject = jsonObject.getAsJsonObject("data").getAsJsonObject("page")

        val seasons = pageObject.getAsJsonArray("containers")
            .filter { it.asJsonObject.getAsString("type") == "episodes" }
            .map { it.asJsonObject }
            .getOrNull(0)
            ?.getAsJsonArray("seasons")
            ?.filter { it.asJsonObject.getAsString("type") == "season" }
            ?.mapNotNull { it.asJsonObject.getAsString("id") } ?: emptyList()

        return pageObject.getAsJsonObject("visuals") to seasons
    }

    suspend fun getEpisodes(accessToken: String, seasonId: String): List<JsonObject> {
        val episodes = mutableListOf<JsonObject>()
        var page = 1
        var hasMore: Boolean

        do {
            val url =
                "https://disney.api.edge.bamgrid.com/explore/v1.4/season/$seasonId?limit=24&offset=${(page++ - 1) * 24}"
            val response = HttpRequest().get(url, mapOf("Authorization" to "Bearer $accessToken"))
            require(response.status.value == 200) { "Failed to fetch Disney+ content" }
            val json = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)

            val jsonObject = json.getAsJsonObject("data").getAsJsonObject("season")
            hasMore = jsonObject.getAsJsonObject("pagination").getAsBoolean("hasMore") ?: false

            jsonObject.getAsJsonArray("items")
                .filter { it.asJsonObject.getAsString("type") == "view" }
                .forEach { episodes.add(it.asJsonObject) }
        } while (hasMore)

        return episodes
    }

    fun getImageUrl(id: String) = "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/$id/compose"
}