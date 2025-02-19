package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.time.Duration

abstract class AbstractDisneyPlusWrapper {
    data class Show(
        val id: String,
        val name: String,
        val image: String,
        val banner: String,
        val description: String?,
        val seasons: Set<String>
    )

    data class Episode(
        val show: Show,
        val id: String,
        val oldId: String,
        val seasonId: String,
        val season: Int,
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
    )

    protected val baseUrl = "https://disney.api.edge.bamgrid.com/"
    protected val httpRequest = HttpRequest()

    @Synchronized
    private fun getAccessToken() = MapCache.getOrCompute(
        "AbstractDisneyPlusWrapper.getAccessToken",
        duration = Duration.ofHours(3).plusMinutes(30),
        classes = listOf(Config::class.java),
        key = ""
    ) {
        val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)

        runBlocking {
            val response = httpRequest.post(
                "${baseUrl}graph/v1/device/graphql",
                headers = mapOf(HttpHeaders.Authorization to (configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_AUTHORIZATION) ?: "")),
                body = ObjectParser.toJson(
                    mapOf(
                        "operationName" to "refreshToken",
                        "query" to "mutation refreshToken(\$input:RefreshTokenInput!){refreshToken(refreshToken:\$input){activeSession{sessionId}}}",
                        "variables" to mapOf(
                            "input" to mapOf(
                                "refreshToken" to configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_REFRESH_TOKEN)
                            )
                        ),
                    )
                )
            )
            require(response.status == HttpStatusCode.OK) { "Failed to get access token (${response.status.value})" }

            ObjectParser.fromJson(response.bodyAsText())
                .getAsJsonObject("extensions")
                .getAsJsonObject("sdk")
                .getAsJsonObject("token")
                .getAsString("accessToken")!!
        }
    }

    protected suspend fun HttpRequest.getWithAccessToken(url: String) = get(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAccessToken()}"))

    abstract suspend fun getShow(id: String): Show
    abstract suspend fun getEpisodesByShowId(locale: String, showId: String): List<Episode>
    abstract suspend fun getShowIdByEpisodeId(episodeId: String): Pair<String, String>

    fun getImageUrl(id: String) = "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/$id/compose"
}