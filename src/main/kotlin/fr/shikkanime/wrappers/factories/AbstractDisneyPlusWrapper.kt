package fr.shikkanime.wrappers.factories

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.time.Duration

abstract class AbstractDisneyPlusWrapper {
    data class Show(
        val id: String,
        val name: String,
        val image: String,
        val banner: String,
        val carousel: String,
        val title: String,
        val description: String?,
        val seasons: Set<String>
    ) : Serializable

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
        val audioLocales: Array<String>
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Episode) return false

            return season == other.season &&
                    number == other.number &&
                    duration == other.duration &&
                    show == other.show &&
                    id == other.id &&
                    oldId == other.oldId &&
                    seasonId == other.seasonId &&
                    title == other.title &&
                    description == other.description &&
                    url == other.url &&
                    image == other.image &&
                    resourceId == other.resourceId &&
                    audioLocales.contentEquals(other.audioLocales)
        }

        override fun hashCode(): Int {
            var result = season
            result = 31 * result + number
            result = 31 * result + duration.hashCode()
            result = 31 * result + show.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + oldId.hashCode()
            result = 31 * result + seasonId.hashCode()
            result = 31 * result + (title?.hashCode() ?: 0)
            result = 31 * result + (description?.hashCode() ?: 0)
            result = 31 * result + url.hashCode()
            result = 31 * result + image.hashCode()
            result = 31 * result + resourceId.hashCode()
            result = 31 * result + audioLocales.contentHashCode()
            return result
        }
    }

    protected val baseUrl = "https://disney.api.edge.bamgrid.com/"
    protected val httpRequest = HttpRequest()

    @Synchronized
    private fun getAccessToken() = MapCache.getOrCompute(
        "AbstractDisneyPlusWrapper.getAccessToken",
        duration = Duration.ofHours(3).plusMinutes(30),
        classes = listOf(Config::class.java),
        typeToken = object : TypeToken<MapCacheValue<String>>() {},
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
                        "query" to $$"mutation refreshToken($input:RefreshTokenInput!){refreshToken(refreshToken:$input){activeSession{sessionId}}}",
                        "variables" to mapOf(
                            "input" to mapOf(
                                "refreshToken" to configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_REFRESH_TOKEN)
                            )
                        ),
                    )
                )
            )
            require(response.status == HttpStatusCode.OK) { "Failed to get access token (${response.status.value})" }

            requireNotNull(ObjectParser.fromJson(response.bodyAsText())
                .getAsJsonObject("extensions")
                .getAsJsonObject("sdk")
                .getAsJsonObject("token")
                .getAsString("accessToken")) { "Access token is null or empty"}
        }
    }

    protected suspend fun HttpRequest.getWithAccessToken(url: String) = get(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAccessToken()}"))
    protected suspend fun HttpRequest.postWithAccessToken(url: String, headers: Map<String, String>, body: String) = post(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAccessToken()}").plus(headers), body = body)

    abstract suspend fun getShow(id: String): Show
    abstract suspend fun getEpisodesByShowId(countryCode: CountryCode, showId: String, checkAudioLocales: Boolean): Array<Episode>
    abstract suspend fun getAudioLocales(countryCode: CountryCode, resourceId: String): Array<String>

    fun getImageUrl(id: String) = "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/$id/compose"
}