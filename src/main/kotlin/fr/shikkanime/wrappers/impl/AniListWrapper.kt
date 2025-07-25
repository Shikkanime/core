package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import io.ktor.client.statement.*
import io.ktor.http.*

object AniListWrapper : AbstractAniListWrapper() {
    private const val RATE_LIMIT = 30 // 30 requests max per minute
    private var lastRequestTime = 0L
    private val logger = LoggerFactory.getLogger(AniListWrapper::class.java)

    private fun throttle() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastRequestTime < 1000L / RATE_LIMIT) {
            val millis = 1000L / RATE_LIMIT - (currentTime - lastRequestTime)
            logger.config("Throttling for ${millis}ms")
            Thread.sleep(millis)
        }

        lastRequestTime = currentTime
    }

    override suspend fun search(
        query: String,
        page: Int,
        limit: Int,
        status: List<Status>
    ): Array<Media> {
        throttle()
        val response = httpRequest.post(baseUrl, mapOf("Content-Type" to "application/json"), ObjectParser.toJson(
            mapOf(
                "query" to $$"query ($search: String, $page: Int, $perPage: Int, $statusIn: [MediaStatus]) { Page(page: $page, perPage: $perPage) { media(search: $search, type: ANIME, sort: SEARCH_MATCH, status_in: $statusIn) { id, idMal, title { romaji english native }, format, genres, episodes, status, externalLinks { type, site, url }, relations { edges { relationType, node { id, title { english, native, romaji }, type, format } } } } } }",
                "variables" to mapOf(
                    "search" to query,
                    "page" to page,
                    "perPage" to limit,
                    "statusIn" to status
                )
            )
        ))
        require(response.status == HttpStatusCode.OK) { "Failed to search media (${response.status.value} - ${response.bodyAsText()})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data").getAsJsonObject("Page").getAsJsonArray("media") ?: throw Exception("Failed to search media")
        return ObjectParser.fromJson(asJsonArray, Array<Media>::class.java)
    }
}