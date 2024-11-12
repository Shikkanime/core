package fr.shikkanime.wrappers

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.LocalDate
import java.time.ZonedDateTime

object AnimationDigitalNetworkWrapper {
    data class Microdata(
        val startDate: ZonedDateTime,
    )

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
        val microdata: Microdata? = null,
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

    private const val BASE_URL = "https://gw.api.animationdigitalnetwork.com/"
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

    suspend fun getShow(id: Int): Show {
        val response = httpRequest.get("${BASE_URL}show/$id?withMicrodata=true")

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to get show")
        }

        val showJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("show")
            ?: throw Exception("Failed to get show")

        return ObjectParser.fromJson(showJson, Show::class.java)
    }

    private suspend fun getShowVideos(showId: Int): Array<Video> {
        val response = httpRequest.get("${BASE_URL}video/show/$showId?order=asc")
        require(response.status == HttpStatusCode.OK) { "Failed to get show videos" }
        val videosJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos") ?: throw Exception("Failed to get videos")
        return ObjectParser.fromJson(videosJson, Array<Video>::class.java)
    }

    suspend fun getPreviousVideo(videoId: Int, showId: Int): Video? {
        val videos = getShowVideos(showId)
        val videoIndex = videos.indexOfFirst { it.id == videoId }

        if (videoIndex == -1 || videoIndex == 0) {
            return null
        }

        return videos.getOrNull(videoIndex - 1)
    }

    suspend fun getNextVideo(videoId: Int, showId: Int): Video? {
        val videos = getShowVideos(showId)
        val videoIndex = videos.indexOfFirst { it.id == videoId }

        if (videoIndex == -1 || videoIndex == videos.size - 1) {
            return null
        }

        return videos.getOrNull(videoIndex + 1)
    }

    suspend fun getVideo(videoId: Int): Video {
        val response = httpRequest.get("${BASE_URL}video/$videoId/public")

        if (response.status.value != 200) {
            throw Exception("Failed to get video")
        }

        val videoJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("video")
            ?: throw Exception("Failed to get video")

        return ObjectParser.fromJson(videoJson, Video::class.java)
    }
}