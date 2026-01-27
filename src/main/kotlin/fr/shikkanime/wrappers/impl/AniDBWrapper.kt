package fr.shikkanime.wrappers.impl

import fr.shikkanime.wrappers.factories.AbstractAniDBWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AniDBWrapper : AbstractAniDBWrapper() {
    override suspend fun getEpisodesByMediaId(id: Int): Array<Episode> {
        val response = httpRequest.get("$baseUrl/anime/$id")
        require(response.status == HttpStatusCode.OK) { "Failed to get episodes for $id (${response.status.value} - ${response.bodyAsText()})" }
        val document = Jsoup.parse(response.bodyAsText())
        val lines = document.select("table.eplist > tbody > tr")

        val episodes = lines.mapNotNull { line ->
            val id = line.select(".eid").text()
                .takeIf { !it.startsWith("OP") && !it.startsWith("ED") && !it.startsWith("C") }
                ?: return@mapNotNull null
            val episodeTitles = line.select(".title > label").attr("title").split(" / ").toSet()
            val airdate = line.select(".airdate").text().takeIf { it.isNotBlank() }
                ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd.MM.yyyy")) } ?: return@mapNotNull null

            Episode(id, episodeTitles, airdate)
        }.toMutableSet()

        if (episodes.any { it.titles == setOf("Complete Movie") }) {
            episodes.removeIf { it.titles.any { "Episode S\\d*".toRegex().matches(it) } }
        }

        return episodes.toTypedArray()
    }
}