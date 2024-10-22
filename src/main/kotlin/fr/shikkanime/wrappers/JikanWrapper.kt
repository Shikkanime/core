package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

object JikanWrapper {
    data class Aired(
        val from: ZonedDateTime?,
        val to: ZonedDateTime?,
    )

    data class Anime(
        @SerializedName("mal_id")
        val malId: Int,
        val url: String,
        val title: String,
        @SerializedName("title_english")
        val titleEnglish: String?,
        @SerializedName("title_japanese")
        val titleJapanese: String?,
        val episodes: Int?,
        val airing: Boolean,
        val aired: Aired?,
    )

    data class Episode(
        @SerializedName("mal_id")
        val malId: Int,
        val title: String,
        val aired: ZonedDateTime?,
    )

    private const val BASE_URL = "https://api.jikan.moe"
    private val httpRequest = HttpRequest()

    suspend fun getAnime(id: Int): Anime {
        val response = httpRequest.get(
            url = "$BASE_URL/v4/anime/$id",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
        )

        if (response.status == HttpStatusCode.TooManyRequests) {
            delay(60 * 1000L)
            return getAnime(id)
        }

        require(response.status == HttpStatusCode.OK) { "Jikan API returned an error: ${response.status.value}" }
        val fromJson = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)
        return ObjectParser.fromJson(fromJson.getAsJsonObject("data"), Anime::class.java)
    }

    suspend fun getEpisodes(animeId: Int, page: Int = 1): List<Episode> {
        val response = httpRequest.get(
            url = "$BASE_URL/v4/anime/$animeId/episodes?page=$page",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString())
        )

        if (response.status == HttpStatusCode.TooManyRequests) {
            delay(60 * 1000L)
            return getEpisodes(animeId, page)
        }

        require(response.status == HttpStatusCode.OK) { "Jikan API returned an error: ${response.status.value}" }

        val jsonResponse = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)
        val totalPages = jsonResponse.getAsJsonObject("pagination")["last_visible_page"].asInt
        val currentPageEpisodes =
            ObjectParser.fromJson(jsonResponse.getAsJsonArray("data"), Array<Episode>::class.java).toList()

        return if (page < totalPages) {
            currentPageEpisodes + getEpisodes(animeId, page + 1)
        } else {
            currentPageEpisodes
        }
    }

    suspend fun getVideosEpisodes(animeId: Int): List<Episode> {
        val response = httpRequest.get(
            url = "$BASE_URL/v4/anime/$animeId/videos/episodes",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
        )

        if (response.status == HttpStatusCode.TooManyRequests) {
            delay(60 * 1000L)
            return getVideosEpisodes(animeId)
        }

        require(response.status == HttpStatusCode.OK) { "Jikan API returned an error: ${response.status.value}" }
        val fromJson = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)
        return ObjectParser.fromJson(fromJson.getAsJsonArray("data"), Array<Episode>::class.java).toList()
    }
}