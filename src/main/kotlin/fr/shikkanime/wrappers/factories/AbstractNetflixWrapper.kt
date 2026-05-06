package fr.shikkanime.wrappers.factories

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.Serializable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

abstract class AbstractNetflixWrapper : IStreamingPlatformWrapper<Int, AbstractNetflixWrapper.Show, AbstractNetflixWrapper.Episode> {
    data class NetflixAuthentification(
        val id: String,
        val secureId: String,
        val authUrl: String
    ) : Serializable

    data class LatestShow(
        val id: Int,
        val title: String,
        val isPlayable: Boolean
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
        override val id: Int,
        val name: String,
        val thumbnail: String?,
        val banner: String,
        val carousel: String,
        val title: String?,
        val description: String?,
        val seasonCount: Int?,
        val availabilityStartTime: ZonedDateTime?,
        val isAvailable: Boolean,
        val isPlayable: Boolean,
        val genres: List<String>,
        val runtimeSec: Long? = null,
        val metadata: ShowMetadata? = null,
        val json: JsonObject? = null,
    ) : Serializable, IStreamingPlatformWrapper.Id<Int>

    data class Season(
        val id: Int,
        val name: String,
        val episodeCount: Int,
    )

    data class Episode(
        val show: Show,
        val oldId: String,
        override val id: Int,
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
    ) : Serializable, IStreamingPlatformWrapper.Id<Int>

    protected val baseUrl = "https://www.netflix.com"
    private val apiUrl = "https://web.prod.cloud.netflix.com/graphql"

    private fun decodeUtf8(input: String) = input.replace("""\\x([0-9A-Fa-f]{2})""".toRegex()) { matchResult ->
        val hexCode = matchResult.groupValues[1]
        hexCode.toInt(16).toChar().toString()
    }

    protected fun extractAuthUrl(html: String): String = decodeUtf8(html.substringAfter("authURL\":\"").substringBefore("\""))

    private suspend fun getNetflixAuthentification() = MapCache.getOrComputeAsync(
        "AbstractNetflixWrapper.getNetflixAuthentification",
        typeToken = object : TypeToken<MapCacheValue<NetflixAuthentification>>() {},
        key = StringUtils.EMPTY_STRING
    ) {
        val (document, cookies) = HttpRequest.getCookies(baseUrl)

        return@getOrComputeAsync NetflixAuthentification(
            requireNotNull(cookies["NetflixId"]),
            requireNotNull(cookies["SecureNetflixId"]),
            extractAuthUrl(document.html())
        )
    }

    protected fun getCookieValue(netflixId: String, netflixSecureId: String): String =
        "NetflixId=${URLDecoder.decode(netflixId, StandardCharsets.UTF_8)}; SecureNetflixId=${
            URLDecoder.decode(
                netflixSecureId,
                StandardCharsets.UTF_8
            )
        }"

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

    abstract suspend fun getLatestShows(): Array<LatestShow>
}