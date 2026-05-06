package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.LocalDate

object AnimationDigitalNetworkWrapper : AbstractAnimationDigitalNetworkWrapper() {
    override suspend fun getLatestEpisodes(locale: String, date: LocalDate): Array<Episode> {
        val response = HttpRequest.getWithHeaders(locale, "${baseUrl}video/calendar?date=$date")
        require(response.status == HttpStatusCode.OK) { "Failed to get media list (${response.status.value})" }
        val videos = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos") ?: throw Exception("Failed to get media list")
        return ObjectParser.fromJson(videos, Array<Episode>::class.java)
    }

    override suspend fun getShow(locale: String, id: Int): Show {
        val response = HttpRequest.getWithHeaders(locale, "${baseUrl}show/$id?withMicrodata=true")
        require(response.status == HttpStatusCode.OK) { "Failed to get show (${response.status.value})" }
        val showJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("show") ?: throw Exception("Failed to get show")
        return ObjectParser.fromJson(showJson, Show::class.java)
    }

    override suspend fun getEpisodesByShowId(locale: String, showId: Int): Array<Episode> {
        val response = HttpRequest.getWithHeaders(locale, "${baseUrl}video/show/$showId?order=asc&limit=-1")
        require(response.status == HttpStatusCode.OK) { "Failed to get show videos (${response.status.value})" }
        val videosJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("videos") ?: throw Exception("Failed to get videos")
        return ObjectParser.fromJson(videosJson, Array<Episode>::class.java)
    }

    override suspend fun getEpisode(locale: String, id: Int): Episode {
         val response = HttpRequest.getWithHeaders(locale, "${baseUrl}video/$id/public")
        require(response.status == HttpStatusCode.OK) { "Failed to get video (${response.status.value})" }
        val videoJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("video") ?: throw Exception("Failed to get video")
        return ObjectParser.fromJson(videoJson, Episode::class.java)
    }
}