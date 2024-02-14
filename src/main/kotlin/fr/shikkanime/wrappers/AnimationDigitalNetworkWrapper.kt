package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import java.time.LocalDate

object AnimationDigitalNetworkWrapper {
    private const val BASE_URL = "https://gw.api.animationdigitalnetwork.fr/"

    suspend fun getLatestVideos(dateString: LocalDate): List<JsonObject> {
        val response = HttpRequest().get("${BASE_URL}video/calendar?date=$dateString")

        if (response.status.value != 200) {
            throw Exception("Failed to get media list")
        }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos")?.map { it.asJsonObject }
            ?: throw Exception("Failed to get media list")
    }

    suspend fun getShow(animeName: String): JsonObject {
        val response = HttpRequest().get("${BASE_URL}show/$animeName?withMicrodata=true&withSeo=true")

        if (response.status.value != 200) {
            throw Exception("Failed to get show id")
        }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("show")
            ?: throw Exception("Failed to get show id")
    }

    suspend fun getShowVideo(videoId: Int): JsonObject {
        val response = HttpRequest().get("${BASE_URL}video/$videoId/public?withMicrodata=true&withSeo=true")

        if (response.status.value != 200) {
            throw Exception("Failed to get video")
        }

        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("video")
            ?: throw Exception("Failed to get video")
    }
}