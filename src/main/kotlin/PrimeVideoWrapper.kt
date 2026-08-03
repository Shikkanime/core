package fr.shikkanime

import fr.shikkanime.models.PrimeVideoDetail
import fr.shikkanime.models.PrimeVideoDetailBody
import fr.shikkanime.models.PrimeVideoDetailWidgets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

private const val BASE_URL = "https://www.primevideo.com"
private const val WEB_APP_CLIENT_VERSION = "1.0.124589.0"

private fun String.toLanguageCode(): String {
    val parsedLocale = Locale.forLanguageTag(this)
    require(parsedLocale.language.isNotBlank()) { "Invalid locale: $this" }
    return parsedLocale.language.lowercase(Locale.ROOT)
}

private fun URLBuilder.addQueryParameters(queryParameters: Map<String, String?>) {
    queryParameters.forEach { (name, value) ->
        value?.takeIf(String::isNotBlank)?.let {
            parameters.append(name, it)
        }
    }
}

class PrimeVideoWrapper(private val httpClient: HttpClient) {
    suspend fun fetchDetailWithId(locale: String, id: String): PrimeVideoDetail<PrimeVideoDetailBody> =
        fetchDetail(locale, "/-/${locale.toLanguageCode()}/detail/$id")

    suspend fun fetchDetail(locale: String, link: String): PrimeVideoDetail<PrimeVideoDetailBody> {
        val languageCode = locale.toLanguageCode()

        val response = httpClient.get("$BASE_URL$link") {
            url {
                addQueryParameters(mapOf("dvWebAppClientVersion" to WEB_APP_CLIENT_VERSION))
            }
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.AcceptLanguage, languageCode)
            header("X-Requested-With", "WebAppSPA")
        }
        require(response.status.isSuccess()) { "Failed to fetch detail: ${response.status} ${response.bodyAsText()}" }
        return response.body<PrimeVideoDetail<PrimeVideoDetailBody>>()
    }

    suspend fun getDetailWidgets(titleId: String, token: String): PrimeVideoDetailWidgets {
        val response = httpClient.get {
            url {
                takeFrom(BASE_URL)
                appendPathSegments("api", "getDetailWidgets")
                addQueryParameters(mapOf(
                    "titleID" to titleId,
                    "widgets" to URLEncoder.encode("[{\"widgetType\":\"EpisodeList\",\"widgetToken\":\"$token\"}]", StandardCharsets.UTF_8)
                ))
            }
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
        require(response.status.isSuccess()) { "Failed to fetch detail widgets: ${response.status} ${response.bodyAsText()}" }
        return response.body<PrimeVideoDetailWidgets>()
    }
}