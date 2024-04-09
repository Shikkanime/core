package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.CountryCode
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

    suspend fun getSeasons(accessToken: String, countryCode: CountryCode, id: String): List<String> {
        val seasonsResponse = HttpRequest().get(
            "https://disney.content.edge.bamgrid.com/svc/content/DmcSeriesBundle/version/5.1/region/${countryCode.name}/audience/k-false,l-true/maturity/1850/language/${countryCode.locale}/encodedSeriesId/$id",
            mapOf("Authorization" to "Bearer $accessToken")
        )

        require(seasonsResponse.status.value == 200) { "Failed to fetch Disney+ content" }
        val seasonsJson = ObjectParser.fromJson(seasonsResponse.bodyAsText(), JsonObject::class.java)
        return seasonsJson.getAsJsonObject("data")
            .getAsJsonObject("DmcSeriesBundle")
            .getAsJsonObject("seasons")
            .getAsJsonArray("seasons")
            .mapNotNull { it.asJsonObject.getAsString("seasonId") }
    }

    suspend fun getEpisodes(accessToken: String, countryCode: CountryCode, seasonId: String): List<JsonObject> {
        val episodes = mutableListOf<JsonObject>()
        var page = 1
        var hasMore: Boolean

        do {
            val url =
                "https://disney.content.edge.bamgrid.com/svc/content/DmcEpisodes/version/5.1/region/${countryCode.name}/audience/k-false,l-true/maturity/1850/language/${countryCode.locale}/seasonId/$seasonId/pageSize/15/page/${page++}"
            val response = HttpRequest().get(url, mapOf("Authorization" to "Bearer $accessToken"))
            require(response.status.value == 200) { "Failed to fetch Disney+ content" }
            val json = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)

            val dmcEpisodesMeta = json.getAsJsonObject("data").getAsJsonObject("DmcEpisodes")
            hasMore = dmcEpisodesMeta.getAsJsonObject("meta").getAsBoolean("hasMore") ?: false
            dmcEpisodesMeta.getAsJsonArray("videos").forEach { episodes.add(it.asJsonObject) }
        } while (hasMore)

        return episodes
    }
}