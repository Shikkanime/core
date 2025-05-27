package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
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
        val resourceId: String,
        val audioLocales: Set<String>
    )

    protected val baseUrl = "https://disney.api.edge.bamgrid.com/"
    protected val httpRequest = HttpRequest()

    @Synchronized
    private fun getAccessToken() = MapCache.getOrCompute(
        "AbstractDisneyPlusWrapper.getAccessToken",
        duration = Duration.ofHours(3).plusMinutes(30),
        classes = listOf(Config::class.java),
        key = StringUtils.EMPTY_STRING
    ) {
        val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)

        runBlocking {
            val response = httpRequest.post(
                "${baseUrl}graph/v1/device/graphql",
                headers = mapOf(HttpHeaders.Authorization to (configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_AUTHORIZATION) ?: StringUtils.EMPTY_STRING)),
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
    protected suspend fun HttpRequest.postWithAccessToken(url: String, headers: Map<String, String>, body: String) = post(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAccessToken()}").plus(headers), body = body)

    abstract suspend fun getShow(id: String): Show
    abstract suspend fun getEpisodesByShowId(locale: String, showId: String, checkAudioLocales: Boolean): List<Episode>
    abstract suspend fun getAudioLocales(resourceId: String): Set<String>

    fun getImageUrl(id: String) = "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/$id/compose"
}