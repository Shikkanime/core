package fr.shikkanime.wrappers.impl

import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.wrappers.factories.AbstractLiveChart
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import java.time.LocalDate

object LiveChartWrapper : AbstractLiveChart() {
    private val platformIdRegex = "https://(?:www\\.)?(?:animationdigitalnetwork|crunchyroll|disneyplus|netflix|primevideo)\\.com/[a-z-]+/(?:entity-)?([^/]+)".toRegex()

    override suspend fun getAnimeIdsFromDate(date: LocalDate): Set<String> {
        val response = httpRequest.get("$baseUrl/schedule?date=${date.atStartOfWeek()}&layout=full&sort=release_date&start=monday")
        require(response.status == HttpStatusCode.OK) { "Failed to fetch schedule: ${response.status}" }
        val document = Jsoup.parse(response.bodyAsText())

        return document.select("article[data-anime-id]:has(a.lc-anime-card--related-links--icon.watch)")
            .map { it.attr("data-anime-id") }
            .toSet()
    }

    override suspend fun getStreamsForAnime(animeId: String): Set<Pair<Platform, String>> {
        val payload = ObjectParser.toJson(mapOf(
            "operationName" to "AnimeStreams",
            "variables" to mapOf("animeId" to animeId, "availableInViewerRegion" to true),
            "query" to $$"""query AnimeStreams($animeId: ID!, $availableInViewerRegion: Boolean) {
                legacyStreams(animeId: $animeId, availableInViewerRegion: $availableInViewerRegion, first: 100) {
                    nodes { id url displayName }
                }
            }"""
        ))
        val streamResponse = httpRequest.post("$baseUrl/graphql", mapOf(
            HttpHeaders.ContentType to ContentType.Application.Json.toString()
        ), payload)
        require(streamResponse.status == HttpStatusCode.OK) { "Failed to fetch streams for anime ID $animeId: ${streamResponse.status}" }

        return ObjectParser.fromJson(streamResponse.bodyAsText())
            .getAsJsonObject("data").getAsJsonObject("legacyStreams")
            .getAsJsonArray("nodes")
            .mapNotNull { node ->
                val platform = Platform.findByName(node.asJsonObject.getAsString("displayName")) ?: return@mapNotNull null
                val url = node.asJsonObject.getAsString("url") ?: return@mapNotNull null
                val platformId = platformIdRegex.find(url)?.groupValues?.get(1) ?: return@mapNotNull null
                platform to platformId
            }.toSet()
    }
}