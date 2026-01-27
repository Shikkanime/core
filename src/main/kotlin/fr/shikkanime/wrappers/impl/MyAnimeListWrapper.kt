package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.factories.AbstractMyAnimeListWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay

object MyAnimeListWrapper : AbstractMyAnimeListWrapper() {
    private const val RATE_LIMIT = 55 // 30 requests max per minute, we use 25 to be safe
    private var lastRequestTime = 0L
    private val logger = LoggerFactory.getLogger(AniListWrapper::class.java)

    private suspend fun throttle() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastRequestTime
        val minInterval = 60_000L / RATE_LIMIT

        if (elapsedTime < minInterval) {
            val waitTime = minInterval - elapsedTime
            logger.config("Throttling requests. Waiting for $waitTime ms")
            delay(waitTime)
        }

        lastRequestTime = System.currentTimeMillis()
    }

    override suspend fun getMediaById(id: Int): Media {
        throttle()
        val response = httpRequest.get("$baseUrl/anime/$id/full")
        require(response.status == HttpStatusCode.OK) { "Failed to get media by ID (${response.status.value} - ${response.bodyAsText()})" }
        val asJsonObject = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data") ?: throw Exception("Failed to get media by ID")
        return ObjectParser.fromJson(asJsonObject, Media::class.java)
    }

    override suspend fun getEpisodesByMediaId(id: Int): Array<Episode> {
        throttle()
        val response = httpRequest.get("$baseUrl/anime/$id/episodes")
        require(response.status == HttpStatusCode.OK) { "Failed to get episodes for $id (${response.status.value} - ${response.bodyAsText()})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get episodes for $id")
        return ObjectParser.fromJson(asJsonArray, Array<Episode>::class.java)
    }
}