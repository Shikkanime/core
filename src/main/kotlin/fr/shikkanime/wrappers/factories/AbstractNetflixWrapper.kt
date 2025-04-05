package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import io.ktor.client.statement.*
import java.time.Duration
import java.time.ZonedDateTime

abstract class AbstractNetflixWrapper {
    data class Show(
        val id: Int,
        val name: String,
        val banner: String,
        val description: String?,
        val seasonCount: Int?,
    )

    data class Season(
        val id: Int,
        val name: String,
        val episodeCount: Int,
    )

    data class Episode(
        val show: Show,
        val oldId: String,
        val id: Int,
        val releaseDateTime: ZonedDateTime,
        val season: Int,
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
    )

    private val baseUrl = "https://www.netflix.com"
    private val apiUrl = "https://web.prod.cloud.netflix.com/graphql"
    protected val httpRequest = HttpRequest()

    private fun getIdAndSecureId() = MapCache.getOrCompute(
        "AbstractNetflixWrapper.getIdAndSecureId",
        duration = Duration.ofDays(1),
        key = ""
    ) {
        val cookies = httpRequest.getCookiesWithBrowser(baseUrl).associateBy { it.name!! }
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
    abstract suspend fun getEpisodesByShowId(locale: String, id: Int): List<Episode>
}