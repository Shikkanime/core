package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay

private const val MEDIA_DEFINITION = """id
                          idMal
                          startDate {
                            year
                          }
                          title {
                            romaji
                            english
                            native
                          }
                          format
                          genres
                          tags {
                            isAdult
                            isGeneralSpoiler
                            isMediaSpoiler
                            name
                            rank
                          }
                          episodes
                          status
                          externalLinks {
                            type
                            site
                            url
                          }
                          relations {
                            edges {
                              relationType
                              node {
                                id
                                startDate {
                                  year
                                }
                                title {
                                  english
                                  native
                                  romaji
                                }
                                type
                                format
                              }
                            }
                          }"""

object AniListWrapper : AbstractAniListWrapper() {
    private const val RATE_LIMIT = 25 // 30 requests max per minute, we use 25 to be safe
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

    override suspend fun search(
        query: String,
        page: Int,
        limit: Int,
        status: List<Status>
    ): Array<Media> {
        throttle()
        val response = httpRequest.post(baseUrl, mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()), ObjectParser.toJson(
            mapOf(
                "query" to $$"""
                    query ($search: String, $page: Int, $perPage: Int, $statusIn: [MediaStatus]) {
                      Page(page: $page, perPage: $perPage) {
                        media(
                          search: $search
                          type: ANIME
                          sort: SEARCH_MATCH
                          status_in: $statusIn
                        ) {
                          $$MEDIA_DEFINITION
                        }
                      }
                    }
                """.trimIndent(),
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

    override suspend fun getMediaById(id: Int): Media {
        throttle()
        val response = httpRequest.post(baseUrl, mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()), ObjectParser.toJson(
            mapOf(
                "query" to $$"""
                    query Media($mediaId: Int) {
                      Media(id: $mediaId) {
                        $$MEDIA_DEFINITION
                      }
                    }
                """.trimIndent(),
                "variables" to mapOf(
                    "mediaId" to id
                )
            )
        ))
        require(response.status == HttpStatusCode.OK) { "Failed to get media by ID (${response.status.value} - ${response.bodyAsText()})" }
        val asJsonObject = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data").getAsJsonObject("Media") ?: throw Exception("Failed to get media by ID")
        return ObjectParser.fromJson(asJsonObject, Media::class.java)
    }
}