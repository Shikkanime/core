package fr.shikkanime.wrappers

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import java.time.LocalDate
import java.time.ZonedDateTime

object AnimationDigitalNetworkWrapper {
    data class Show(
        val id: Int,
        val shortTitle: String?,
        val title: String,
        val image2x: String,
        val imageHorizontal2x: String,
        val summary: String?,
        val genres: List<String> = emptyList(),
        val simulcast: Boolean,
        val firstReleaseYear: String,
    )

    data class Video(
        val id: Int,
        val title: String,
        val season: String?,
        val releaseDate: ZonedDateTime,
        val shortNumber: String?,
        val type: String,
        val name: String?,
        val summary: String?,
        val image2x: String,
        val url: String,
        val duration: Long,
        val languages: List<String> = emptyList(),
        val show: Show,
    )

    private const val BASE_URL = "https://gw.api.animationdigitalnetwork.fr/"
    private val httpRequest = HttpRequest()

    suspend fun getLatestVideos(date: LocalDate): Array<Video> {
        val response = httpRequest.get("${BASE_URL}video/calendar?date=$date")

        if (response.status.value != 200) {
            throw Exception("Failed to get media list")
        }

        val videos = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos")
            ?: throw Exception("Failed to get media list")

        return ObjectParser.fromJson(videos, Array<Video>::class.java)
    }

    suspend fun getShowVideo(videoId: String): Video {
        val response = httpRequest.get("${BASE_URL}video/$videoId/public")

        if (response.status.value != 200) {
            throw Exception("Failed to get video")
        }

        val videoJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("video")
            ?: throw Exception("Failed to get video")

        return ObjectParser.fromJson(videoJson, Video::class.java)
    }
}