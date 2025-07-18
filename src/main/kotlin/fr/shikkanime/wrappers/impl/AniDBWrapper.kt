package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.FileManager
import fr.shikkanime.wrappers.factories.AbstractAniDBWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate

object AniDBWrapper : AbstractAniDBWrapper() {
    override suspend fun getAnimeTitles(): Document {
        val response = httpRequest.get("https://anidb.net/api/anime-titles.xml.gz")
        require(response.status == HttpStatusCode.OK) { "Failed to get anime titles" }
        return Jsoup.parse(String(FileManager.decompressGzip(response.bodyAsBytes())))
    }

    override suspend fun getAnimeDetails(clientId: String, animeId: Int): Document {
        val response = httpRequest.get("http://api.anidb.net:9001/httpapi?request=anime&client=$clientId&clientver=1&protover=1&aid=$animeId")
        require(response.status == HttpStatusCode.OK) { "Failed to get anime" }
        return Jsoup.parse(response.bodyAsText())
    }

    override suspend fun getEpisodesByAnime(clientId: String, animeId: Int): List<Episode> {
        val episodes = getAnimeDetails(clientId, animeId).select("episodes > episode")

        return episodes.mapNotNull { episode ->
            val airDateString = episode.select("airdate").text().ifBlank { return@mapNotNull null }

            Episode(
                id = episode.attr("id").toInt(),
                resources = episode.select("resources > resource").associate {
                    it.attr("type").toInt() to it.select("identifier").map { identifier -> identifier.text() }.toSet()
                },
                number = episode.select("epno").text(),
                airdate = LocalDate.parse(airDateString)
            )
        }.sortedBy { it.airdate }
    }
}