package fr.shikkanime

import fr.shikkanime.utils.HttpRequest
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import java.time.LocalDate

data class NatalieNews(
    val tags: Set<String>,
    val title: String,
    val shortDescription: String,
    val url: String,
    val pubDate: String,
    var relatedNews: Set<NatalieNews> = emptySet()
)

suspend fun main() {
    val now = LocalDate.now()
    val httpRequest = HttpRequest()

    val response = httpRequest.get("https://natalie.mu/comic/tag/50/page/1")
    require(response.status == HttpStatusCode.OK) { "Failed to fetch data (${response.status.value})" }
    val document = Jsoup.parse(response.bodyAsText())
    // アニメ
    val newsElements = document.select(".NA_card_wrapper > .NA_card-l")
    val toRegex = "「(.*?)」".toRegex()

    val news = newsElements.map { element ->
        val tags = element.select(".NA_card_link-tag > a").map { it.text() }.toSet()
        val title = element.select(".NA_card_title").text()
        val shortDescription = element.select(".NA_card_summary").text()
        val url = element.select(".NA_card-l > a").attr("href")
        val pubDate = element.select(".NA_card_date").text()

        val news = NatalieNews(
            tags,
            title,
            shortDescription,
            url,
            pubDate
        )

        val animeTitleInTitle = toRegex.find(title)?.groupValues[1]
        val animeTitlesInShortDescription = toRegex.find(shortDescription)?.groupValues[1]
        val animeTitles = setOfNotNull(animeTitleInTitle, animeTitlesInShortDescription)
        println("animeTitles: $animeTitles")

        // Calculate related news
        val detailsNews = httpRequest.get(url)
        require(detailsNews.status == HttpStatusCode.OK) { "Failed to fetch data (${detailsNews.status.value})" }
        val detailsDocument = Jsoup.parse(detailsNews.bodyAsText())

        val relatedNewsElements = detailsDocument.select(".NA_article .NA_card-m")
            .filter {
                val cardTitle = it.select(".NA_card_title").text()
                animeTitles.isNotEmpty() && animeTitles.any { cardTitle.contains(it) }
            }

        println("relatedNewsElements size: ${relatedNewsElements.size}")

        news.relatedNews = relatedNewsElements.map { relatedNewsElement ->
            NatalieNews(
                relatedNewsElement.select(".NA_card_link-tag > a").map { it.text() }.toSet(),
                relatedNewsElement.select(".NA_card_title").text(),
                "",
                relatedNewsElement.select(".NA_card-m > a").attr("href"),
                relatedNewsElement.select(".NA_card_date").text()
            )
        }.sortedByDescending {
            // Example
            // 2024年11月1日
            // 7月18日
            if (pubDate.isBlank()) {
                return@sortedByDescending now
            }

            parseJapaneseDate(it.pubDate, now)
        }.toSet()

        news
    }

    news.filter { n -> n.relatedNews.isEmpty() || n.relatedNews.none { rn -> rn.tags.contains("アニメ化") && parseJapaneseDate(rn.pubDate, now) < parseJapaneseDate(n.pubDate, now)  } }
        .forEach { println(it) }

    httpRequest.close()
}

private fun parseJapaneseDate(pubDate: String, now: LocalDate): LocalDate {
    val year = pubDate.split("年").getOrNull(0)?.toIntOrNull() ?: now.year
    val month = pubDate.split("月")[0].substringAfter("年").toIntOrNull() ?: now.monthValue
    val day = pubDate.split("日")[0].substringAfter("月").toIntOrNull() ?: now.dayOfMonth

    return LocalDate.of(year, month, day)
}
