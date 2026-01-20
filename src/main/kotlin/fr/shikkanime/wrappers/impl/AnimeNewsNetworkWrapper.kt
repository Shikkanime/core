package fr.shikkanime.wrappers.impl

import fr.shikkanime.wrappers.factories.AbstractAnimeNewsNetworkWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AnimeNewsNetworkWrapper : AbstractAnimeNewsNetworkWrapper() {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getMediaById(id: Int): Media {
        val response = httpRequest.get("$baseUrl/encyclopedia/anime.php?id=$id")
        require(response.status == HttpStatusCode.OK) { "Failed to get media by ID (${response.status.value} - ${response.bodyAsText()})" }
        val document = Jsoup.parse(response.bodyAsText())
        val titles =
            document.select("#infotype-2 > div.tab").map { it.text().replace("(.*)$".toRegex(), "").trim() }.toSet()
        require(titles.isNotEmpty()) { "Failed to find titles for media ID $id" }
        val vintage =
            requireNotNull(document.selectFirst("#infotype-7")) { "Failed to find vintage information for media ID $id" }

        val rangeRegex = "(\\d{4}-\\d{2}-\\d{2}) to (\\d{4}-\\d{2}-\\d{2})".toRegex()

        val spanElement = vintage.selectFirst("span")
        val (from, to) = if (spanElement != null) {
            LocalDate.parse(spanElement.text(), dateFormatter) to null
        } else {
            val tagText = vintage.selectFirst("div.tag")?.text().orEmpty()

            rangeRegex.find(tagText)?.destructured?.let { (start, end) ->
                LocalDate.parse(start) to LocalDate.parse(end)
            } ?: (null to null)
        }

        return Media(titles, from, to)
    }

    override suspend fun getEpisodesByMediaId(id: Int): Array<Episode> {
        val response = httpRequest.get("$baseUrl/encyclopedia/anime.php?id=$id&page=25")
        require(response.status == HttpStatusCode.OK) { "Failed to get episodes for $id (${response.status.value} - ${response.bodyAsText()})" }
        val document = Jsoup.parse(response.bodyAsText())
        val lines = document.select("table.episode-list > tbody > tr")

        return lines.mapNotNull { line ->
            var id = line.select(".n").text().substringBeforeLast('.')
            val hasPoint5 = line.select(".pn").text().isNotBlank()
            if (hasPoint5) id += ".5"
            val titleWrapper = line.select("td[valign=\"top\"]").first()!!
            val alternativeTitleWrapper = titleWrapper.selectFirst("div.j")!!.children()
            val episodeTitles = setOfNotNull(
                titleWrapper.selectFirst("div")!!.text()
            ) + alternativeTitleWrapper.mapNotNull { it.text() }
            val airdate = line.selectFirst(".d")!!.text().takeIf { it.isNotBlank() }
                ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd")) } ?: return@mapNotNull null

            Episode(id, episodeTitles, airdate)
        }.toTypedArray()
    }
}