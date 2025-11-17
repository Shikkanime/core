package fr.shikkanime.wrappers.impl

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.time.LocalDate

object AnimationDigitalNetworkWrapper : AbstractAnimationDigitalNetworkWrapper() {
    override suspend fun getLatestVideos(countryCode: CountryCode, date: LocalDate): Array<Video> {
        val response = httpRequest.get("${baseUrl}video/calendar?date=$date", mapOf("X-Source" to "Web", "X-Target-Distribution" to countryCode.name.lowercase()))
        require(response.status == HttpStatusCode.OK) { "Failed to get media list (${response.status.value})" }
        val videos = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos") ?: throw Exception("Failed to get media list")
        return ObjectParser.fromJson(videos, Array<Video>::class.java)
    }

    override suspend fun getShow(countryCode: CountryCode, id: Int): Show {
        val response = httpRequest.get("${baseUrl}show/$id?withMicrodata=true", mapOf("X-Source" to "Web", "X-Target-Distribution" to countryCode.name.lowercase()))
        require(response.status == HttpStatusCode.OK) { "Failed to get show (${response.status.value})" }
        val showJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("show") ?: throw Exception("Failed to get show")
        return ObjectParser.fromJson(showJson, Show::class.java)
    }

    override suspend fun getShowVideos(countryCode: CountryCode, id: Int): Array<Video> {
        val response = httpRequest.get("${baseUrl}video/show/$id?order=asc&limit=-1", mapOf("X-Source" to "Web", "X-Target-Distribution" to countryCode.name.lowercase()))
        require(response.status == HttpStatusCode.OK) { "Failed to get show videos (${response.status.value})" }
        val videosJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos") ?: throw Exception("Failed to get videos")
        return ObjectParser.fromJson(videosJson, Array<Video>::class.java)
    }

    override suspend fun getVideo(countryCode: CountryCode, id: Int): Video {
         val response = httpRequest.get("${baseUrl}video/$id/public", mapOf("X-Source" to "Web", "X-Target-Distribution" to countryCode.name.lowercase()))
        require(response.status == HttpStatusCode.OK) { "Failed to get video (${response.status.value})" }
        val videoJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("video") ?: throw Exception("Failed to get video")
        return ObjectParser.fromJson(videoJson, Video::class.java)
    }
}