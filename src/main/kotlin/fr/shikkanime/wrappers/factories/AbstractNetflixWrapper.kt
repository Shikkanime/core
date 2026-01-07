package fr.shikkanime.wrappers.factories

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.Serializable
import java.time.ZonedDateTime

abstract class AbstractNetflixWrapper {
    data class NetflixAuthentification(
        val id: String,
        val secureId: String,
        val authUrl: String
    ) : Serializable

    data class EpisodeMetadata(
        val id: Int,
        val image: String?
    ) : Serializable

    data class ShowMetadata(
        val thumbnail: String?,
        val banner: String?,
        val carousel: String?,
        val episodes: List<EpisodeMetadata>,
    ) : Serializable

    data class Show(
        val id: Int,
        val name: String,
        val thumbnail: String?,
        val banner: String,
        val carousel: String,
        val title: String,
        val description: String?,
        val seasonCount: Int?,
        val availabilityStartTime: ZonedDateTime?,
        val isAvailable: Boolean,
        val isPlayable: Boolean,
        val runtimeSec: Long? = null,
        val metadata: ShowMetadata? = null,
        val json: JsonObject? = null,
    ) : Serializable

    data class Season(
        val id: Int,
        val name: String,
        val episodeCount: Int,
    )

    data class Episode(
        val show: Show,
        val oldId: String,
        val id: Int,
        val releaseDateTime: ZonedDateTime?,
        val season: Int,
        val episodeType: EpisodeType,
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
        val audioLocales: Set<String>,
    ) : Serializable

    protected val baseUrl = "https://www.netflix.com"
    private val apiUrl = "https://web.prod.cloud.netflix.com/graphql"
    protected val httpRequest = HttpRequest()

    private fun decodeUtf8(input: String) = input.replace("""\\x([0-9A-Fa-f]{2})""".toRegex()) { matchResult ->
        val hexCode = matchResult.groupValues[1]
        hexCode.toInt(16).toChar().toString()
    }

    protected fun extractAuthUrl(html: String): String = decodeUtf8(html.substringAfter("authURL\":\"").substringBefore("\""))

    @Synchronized
    private fun getNetflixAuthentification() = MapCache.getOrCompute(
        "AbstractNetflixWrapper.getNetflixAuthentification",
        typeToken = object : TypeToken<MapCacheValue<NetflixAuthentification>>() {},
        key = StringUtils.EMPTY_STRING
    ) {
        val documentAndCookies = HttpRequest().use { it.getCookiesWithBrowser(baseUrl) }
        val cookies = documentAndCookies.second.associateBy { cookie -> cookie.name!! }

        return@getOrCompute NetflixAuthentification(
            requireNotNull(cookies["NetflixId"]?.value),
            requireNotNull(cookies["SecureNetflixId"]?.value),
            extractAuthUrl(documentAndCookies.first.html())
        )
    }

    protected fun getCookieValue(netflixId: String, netflixSecureId: String): String =
        "NetflixId=$netflixId; SecureNetflixId=$netflixSecureId"

    protected suspend fun HttpRequest.postGraphQL(countryCode: CountryCode, body: String) = postGraphQL(countryCode.locale, body)

    protected suspend fun HttpRequest.postGraphQL(locale: String, body: String): HttpResponse {
        val (id, secureId) = getNetflixAuthentification()

        return post(
            apiUrl,
            headers = mapOf(
                HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                HttpHeaders.Cookie to getCookieValue(id, secureId),
                "x-netflix.context.locales" to locale,
            ),
            body = body
        )
    }

    abstract suspend fun getShow(locale: String, id: Int): Show
    abstract suspend fun getEpisodesByShowId(countryCode: CountryCode, id: Int): Array<Episode>

    suspend fun getPreviousEpisode(countryCode: CountryCode, showId: Int, episodeId: Int): Episode? {
        val episodes = getEpisodesByShowId(countryCode, showId)
        val episodeIndex = episodes.indexOfFirst { it.id == episodeId }
        require(episodeIndex != -1) { "Episode not found" }
        return if (episodeIndex == 0) null else episodes[episodeIndex - 1]
    }

    suspend fun getNextEpisode(countryCode: CountryCode, showId: Int, episodeId: Int): Episode? {
        val episodes = getEpisodesByShowId(countryCode, showId)
        val episodeIndex = episodes.indexOfFirst { it.id == episodeId }
        require(episodeIndex != -1) { "Episode not found" }
        return if (episodeIndex == episodes.size - 1) null else episodes[episodeIndex + 1]
    }
}