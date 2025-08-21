package fr.shikkanime.wrappers.factories

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.*
import java.io.Serializable
import java.time.ZonedDateTime

abstract class AbstractNetflixWrapper {
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
        val description: String?,
        val seasonCount: Int?,
        val availabilityStartTime: ZonedDateTime?,
        val isAvailable: Boolean,
        val isPlayable: Boolean,
        val runtimeSec: Long? = null,
        val metadata: ShowMetadata? = null,
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
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long
    ) : Serializable

    protected val baseUrl = "https://www.netflix.com"
    private val apiUrl = "https://web.prod.cloud.netflix.com/graphql"
    protected val httpRequest = HttpRequest()

    @Synchronized
    private fun getIdAndSecureId() = MapCache.getOrCompute(
        "AbstractNetflixWrapper.getIdAndSecureId",
        typeToken = object : TypeToken<MapCacheValue<Pair<String?, String?>>>() {},
        key = StringUtils.EMPTY_STRING
    ) {
        val cookies = HttpRequest().use { it.getCookiesWithBrowser(baseUrl).associateBy { cookie -> cookie.name!! } }
        return@getOrCompute cookies["NetflixId"]?.value to cookies["SecureNetflixId"]?.value
    }

    protected suspend fun HttpRequest.postGraphQL(locale: String, body: String): HttpResponse {
        val (id, secureId) = getIdAndSecureId()

        return post(
            apiUrl,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Cookie" to "NetflixId=$id; SecureNetflixId=$secureId",
                "x-netflix.context.locales" to locale,
            ),
            body = body
        )
    }

    abstract suspend fun getShow(locale: String, id: Int): Show
    abstract suspend fun getEpisodesByShowId(locale: String, id: Int): Array<Episode>
}