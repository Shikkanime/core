package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.wrappers.factories.AbstractShikkanimeWorkerWrapper
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

object ShikkanimeWorkerWrapper : AbstractShikkanimeWorkerWrapper() {
    override suspend fun getNetflixEpisodes(
        netflixId: String,
        secureNetflixId: String,
        vararg ids: Int
    ): List<Episode> {
        val response = HttpRequest.post(
            "$baseUrl/netflix-episodes",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
            timeout = 300_000,
            body = Request(
                ids = ids.toList(),
                netflixId = netflixId,
                secureNetflixId = secureNetflixId
            )
        )
        require(response.status == HttpStatusCode.OK) { "Failed to get episodes (${response.status.value} - ${response.bodyAsText()})" }
        return response.body<List<Episode>>()
    }
}