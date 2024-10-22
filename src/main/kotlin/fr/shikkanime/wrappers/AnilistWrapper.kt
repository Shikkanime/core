package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay

object AnilistWrapper {
    data class Title(
        val romaji: String?,
        val english: String?,
    )

    data class Media(
        val id: Int,
        val idMal: Int?,
        val title: Title,
    )

    private const val BASE_URL = "https://graphql.anilist.co"
    private val httpRequest = HttpRequest()

    suspend fun getMedia(name: String): Media {
        val response = httpRequest.post(
            url = BASE_URL,
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
            body = ObjectParser.toJson(
                mapOf(
                    "query" to "query { Media(search: ${ObjectParser.toJson(name)}, type: ANIME) { id, idMal, title { romaji, english } } }"
                )
            ),
        )

        val fromJson = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)

        if (response.status == HttpStatusCode.TooManyRequests ||
            fromJson.getAsJsonArray("errors")
                ?.firstOrNull()?.asJsonObject?.get("status")?.asInt == HttpStatusCode.TooManyRequests.value
        ) {
            val retryAfter = response.headers["Retry-After"]?.toIntOrNull() ?: 60
            delay(retryAfter * 1000L)
            return getMedia(name)
        }

        require(response.status == HttpStatusCode.OK) { "Anilist API returned an error: ${fromJson.getAsJsonArray("errors")}" }
        return ObjectParser.fromJson(fromJson.getAsJsonObject("data").getAsJsonObject("Media"), Media::class.java)
    }
}