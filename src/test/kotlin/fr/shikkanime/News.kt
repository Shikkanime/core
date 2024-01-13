package fr.shikkanime

import com.google.gson.JsonArray
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.Serializable
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

enum class NewsPlatform(val locales: Set<String>) {
    ANIME_NEWS_NETWORK(setOf("fr-FR")),
    ADALA_NEWS(setOf("fr-FR")),
    ANIMOTAKU(setOf("fr-FR")),
    CRUNCHYROLL(setOf("fr-FR", "en-US")),
    GAAK(setOf("fr-FR")),

    ANIME_TV(setOf("en-US")),
    ANITRENDZ(setOf("en-US")),
    ANIMECORNER(setOf("en-US")),
    ;
}

private val toRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
private fun CharSequence.unaccented() = toRegex.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "")
private val separatorPattern = Pattern.compile("[ .,!?:;()\\[\\]{}<>'’“”/]")

data class News(
    val platform: NewsPlatform,
    val locale: String,
    val title: String,
    val releaseDateTime: ZonedDateTime,
    val link: String,
    val image: String,
    val description: String,
) : Serializable {
    @Transient
    lateinit var normalizedDescriptionWords: Set<String>

    fun normalizeDescription() {
        normalizedDescriptionWords = description.split(separatorPattern)
            .asSequence()
            .map { it.lowercase().unaccented() }
            .filter { it.isNotBlank() }
            .toSet()
    }
}

private fun ZonedDateTime.isInRange(z0: ZonedDateTime, z1: ZonedDateTime): Boolean = this.isAfter(z0) && this.isBefore(z1)
private fun format(text: String) = text.replace("\n", " ").replace(System.lineSeparator(), " ").substringBefore("Source").trim()

suspend fun scanNewsPlatform(httpRequest: HttpRequest, limit: Int, platformNews: Map<NewsPlatform, Set<News>>, newsPlatform: NewsPlatform): List<News> {
    val news = platformNews[newsPlatform] ?: emptySet()

    return when (newsPlatform) {
        NewsPlatform.ANIME_NEWS_NETWORK -> {
            val content = httpRequest.get("https://www.animenewsnetwork.com/all/rss.xml?ann-edition=fr").bodyAsText()
                .replace(System.lineSeparator(), "").replace("\n", "")

            "<item>(.*?)</item>".toRegex()
                .findAll(content)
                .map { it.value }
                .toList()
                .take(limit)
                .mapNotNull {
                    val xml = Jsoup.parse(it)
                    val category = xml.getElementsByTag("category").text()

                    if (category.contains("Anime", true).not() && category.contains("Manga", true).not()) {
                        return@mapNotNull null
                    }

                    val title = xml.getElementsByTag("title").text()
                    val releaseDateTime = ZonedDateTime.parse(xml.getElementsByTag("pubDate").text(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    val link = "<link>(.*?)</link>".toRegex().find(it)!!.groupValues[1]

                    if (news.any { n -> n.link == link }) {
                        return@mapNotNull null
                    }

                    val newsDom = Jsoup.parse(httpRequest.get(link).bodyAsText())
                    val description = newsDom.select("#content-zone > div > div.text-zone.easyread-width").text()

                    News(
                        platform = newsPlatform,
                        locale = "fr-FR",
                        title = title,
                        releaseDateTime = releaseDateTime,
                        link = link,
                        image = newsDom.select("link[rel='feed_image']").attr("href"),
                        description = format(description),
                    )
                }
                .filter { n -> news.none { it.link == n.link } }
        }
        NewsPlatform.ADALA_NEWS -> {
            val content = Jsoup.parse(httpRequest.get("https://adala-news.fr/").bodyAsText())

            content.select(".list-post.penci-item-listp").mapNotNull {
                val category = it.select(".cat").text()

                if (category.contains("Anime", true).not()) {
                    return@mapNotNull null
                }

                val titleElement = it.select(".penci-entry-title.entry-title.grid-title")
                val title = titleElement.text()
                val releaseDateTime = ZonedDateTime.parse(it.select(".entry-date.published").attr("datetime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val link = titleElement.select("a").attr("href")

                if (news.any { n -> n.link == link }) {
                    return@mapNotNull null
                }

                val newsDom = Jsoup.parse(httpRequest.get(link).bodyAsText())
                val description = newsDom.select("#penci-post-entry-inner").text()

                News(
                    platform = newsPlatform,
                    locale = "fr-FR",
                    title = title,
                    releaseDateTime = releaseDateTime,
                    link = link,
                    image = newsDom.select("meta[property='og:image']").attr("content"),
                    description = format(description),
                )
            }.filter { n -> news.none { it.link == n.link } }
        }
        NewsPlatform.ANIMOTAKU -> {
            ObjectParser.fromJson(
                httpRequest.get("https://animotaku.fr/wp-json/wp/v2/posts?per_page=$limit&categories=26").bodyAsText(),
                JsonArray::class.java
            ).mapNotNull {
                val element = it.asJsonObject
                val title = element["title"].asJsonObject["rendered"].asString
                val releaseDateTime = ZonedDateTime.parse(element["date_gmt"].asString + "Z")
                val link = element["link"].asString.removeSuffix("/")

                if (news.any { n -> n.link == link }) {
                    return@mapNotNull null
                }

                val image = element["yoast_head_json"].asJsonObject["og_image"].asJsonArray[0].asJsonObject["url"].asString
                val descriptionElement = Jsoup.parse(element["content"].asJsonObject["rendered"].asString)
                val description = descriptionElement.text()

                News(
                    platform = newsPlatform,
                    locale = "fr-FR",
                    title = title,
                    releaseDateTime = releaseDateTime,
                    link = link,
                    image = image,
                    description = format(description),
                )
            }
                .filter { n -> news.none { it.link == n.link } }
        }
        NewsPlatform.CRUNCHYROLL -> {
            newsPlatform.locales.flatMap { locale ->
                val content = httpRequest.get("https://cr-news-api-service.prd.crunchyrollsvc.com/v1/$locale/rss").bodyAsText()
                    .replace(System.lineSeparator(), "").replace("\n", "")

                "<item>(.*?)</item>".toRegex()
                    .findAll(content)
                    .map { match -> match.value }
                    .toList()
                    .take(limit)
                    .mapNotNull {
                        val xml = Jsoup.parse(it)
                        val title = xml.getElementsByTag("title").text()
                        val releaseDateTime = ZonedDateTime.parse(xml.getElementsByTag("pubDate").text(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        val link = xml.getElementsByTag("guid").text()

                        if (news.any { n -> n.link == link }) {
                            return@mapNotNull null
                        }

                        val image = xml.getElementsByTag("media:thumbnail").attr("url")
                        val description = xml.getElementsByTag("content:encoded").text()

                        News(
                            platform = newsPlatform,
                            locale = locale,
                            title = title,
                            releaseDateTime = releaseDateTime,
                            link = link,
                            image = image,
                            description = format(description),
                        )
                    }
                    .filter { n -> news.none { it.link == n.link } }
            }
        }
        NewsPlatform.ANIME_TV -> {
            val content = httpRequest.get("https://animetv-jp.net/feed/").bodyAsText()
                .replace(System.lineSeparator(), "").replace("\n", "")

            "<item>(.*?)</item>".toRegex()
                .findAll(content)
                .map { it.value }
                .toList()
                .take(limit)
                .mapNotNull {
                    val xml = Jsoup.parse(it)
                    val title = xml.getElementsByTag("title").text()
                    val releaseDateTime = ZonedDateTime.parse(xml.getElementsByTag("pubDate").text(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    val link = "<link>(.*?)</link>".toRegex().find(it)!!.groupValues[1]

                    if (news.any { n -> n.link == link }) {
                        return@mapNotNull null
                    }

                    val image = Jsoup.parse(httpRequest.get(link).bodyAsText()).select("meta[property='og:image']").attr("content")
                    val description = Jsoup.parse(xml.getElementsByTag("content:encoded").text()).text()

                    News(
                        platform = newsPlatform,
                        locale = "en-US",
                        title = title,
                        releaseDateTime = releaseDateTime,
                        link = link,
                        image = image,
                        description = format(description),
                    )
                }
                .filter { n -> news.none { it.link == n.link } }
        }
        NewsPlatform.GAAK -> {
            val content = httpRequest.get("https://gaak.fr/category/news/animes/feed/").bodyAsText()
                .replace(System.lineSeparator(), "").replace("\n", "")

            "<item>(.*?)</item>".toRegex()
                .findAll(content)
                .map { it.value }
                .toList()
                .take(limit)
                .mapNotNull {
                    val xml = Jsoup.parse(it)
                    val title = xml.getElementsByTag("title").text()
                    val releaseDateTime = ZonedDateTime.parse(xml.getElementsByTag("pubDate").text(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    val link = "<link>(.*?)</link>".toRegex().find(it)!!.groupValues[1]

                    if (news.any { n -> n.link == link }) {
                        return@mapNotNull null
                    }

                    val newsDom = Jsoup.parse(httpRequest.get(link).bodyAsText())
                    val description = Jsoup.parse(xml.getElementsByTag("content:encoded").text()).text()

                    News(
                        platform = newsPlatform,
                        locale = "fr-FR",
                        title = title,
                        releaseDateTime = releaseDateTime,
                        link = link,
                        image = newsDom.select("meta[property='og:image']").attr("content"),
                        description = format(description),
                    )
                }
                .filter { n -> news.none { it.link == n.link } }
        }
        NewsPlatform.ANITRENDZ -> {
            val content = httpRequest.get("https://anitrendz.net/news/feed/").bodyAsText()
                .replace(System.lineSeparator(), "").replace("\n", "")

            "<item>(.*?)</item>".toRegex()
                .findAll(content)
                .map { it.value }
                .toList()
                .take(limit)
                .mapNotNull {
                    val xml = Jsoup.parse(it)
                    val category = xml.getElementsByTag("category").text()

                    if (category.contains("Anime", true).not()) {
                        return@mapNotNull null
                    }

                    val title = xml.getElementsByTag("title").text()
                    val releaseDateTime = ZonedDateTime.parse(xml.getElementsByTag("pubDate").text(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    val link = "<link>(.*?)</link>".toRegex().find(it)!!.groupValues[1]

                    if (news.any { n -> n.link == link }) {
                        return@mapNotNull null
                    }

                    val newsDom = Jsoup.parse(httpRequest.get(link).bodyAsText())
                    val description = Jsoup.parse(xml.getElementsByTag("content:encoded").text()).text()

                    News(
                        platform = newsPlatform,
                        locale = "en-US",
                        title = title,
                        releaseDateTime = releaseDateTime,
                        link = link,
                        image = newsDom.select("meta[property='og:image']").attr("content"),
                        description = format(description),
                    )
                }
                .filter { n -> news.none { it.link == n.link } }
        }
        NewsPlatform.ANIMECORNER -> {
            val content = httpRequest.get("https://animecorner.me/feed/").bodyAsText()
                .replace(System.lineSeparator(), "").replace("\n", "")

            "<item>(.*?)</item>".toRegex()
                .findAll(content)
                .map { it.value }
                .toList()
                .take(limit)
                .mapNotNull {
                    val xml = Jsoup.parse(it)
                    val category = xml.getElementsByTag("category").text()

                    if (category.contains("Anime", true).not()) {
                        return@mapNotNull null
                    }

                    val title = xml.getElementsByTag("title").text()
                    val releaseDateTime = ZonedDateTime.parse(xml.getElementsByTag("pubDate").text(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    val link = "<link>(.*?)</link>".toRegex().find(it)!!.groupValues[1]

                    if (news.any { n -> n.link == link }) {
                        return@mapNotNull null
                    }

                    val newsDom = Jsoup.parse(httpRequest.get(link).bodyAsText())
                    val description = newsDom.select("#penci-post-entry-inner").text()

                    News(
                        platform = newsPlatform,
                        locale = "en-US",
                        title = title,
                        releaseDateTime = releaseDateTime,
                        link = link,
                        image = newsDom.select("meta[property='og:image']").attr("content"),
                        description = format(description),
                    )
                }
                .filter { n -> news.none { it.link == n.link } }
        }
    }
}

suspend fun main() {
    val limit = 20
    val newsRange = 3L
    val httpRequest = HttpRequest()
    val file = File("news.shikk")
    val onlyCheckLocale: String? = "fr-FR"

    val platformNews = if (file.exists())
        FileManager.readFile<Array<News>>(file)
            .groupBy { it.platform }
            .mapValues { (_, news) -> news.toMutableSet() }
            .toMutableMap()
    else
        mutableMapOf()

    val intersectSimilarityThreshold = 0.21

    NewsPlatform.entries.forEach { newsPlatform ->
        val news = scanNewsPlatform(httpRequest, limit, platformNews, newsPlatform)
        val historyNews = platformNews.getOrDefault(newsPlatform, mutableSetOf())
        historyNews.addAll(news)
        platformNews[newsPlatform] = historyNews
    }


    if (file.exists().not())
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }

    val flatten = platformNews.values.flatten()
    FileManager.writeFile(file, flatten.toTypedArray())
    flatten.forEach { it.normalizeDescription() }

    val predicate: (News) -> Boolean = { onlyCheckLocale == null || it.locale == onlyCheckLocale }
    val similarNews = mutableSetOf<Set<News>>()

    NewsPlatform.entries.forEach { newsPlatform ->
        val news = (platformNews[newsPlatform] ?: emptySet()).filter(predicate).toSet()
        val otherNews = platformNews.filterKeys { it != newsPlatform }.values.flatten().filter(predicate).toSet()
        findIntersectSimilarNews(news, otherNews, similarNews, intersectSimilarityThreshold, newsRange)
    }

    println("Similar news detected: ${similarNews.flatten().size}")

    val defaultPrompt = """
        Condense les actualités suivantes en une seule. Elles portent sur le même sujet. L'actualité résumée sera publiée sur mes réseaux sociaux, donc rend la pertinente et la courte possible sans inventer.
        Sers-toi uniquement des informations qui te sont fournies. Écrit là de manière formelle, tout en gardant une forme de synthèse, avec un seul paragraphe, le plus court possible.
        N'utilises pas de hashtag, je les ajouterai moi-même. Ecrit moi directement le texte de l'actualité résumée.
        
        Voici le format que j'attends :
        {"title": "<Titre de l'actualité>","description": "<Description de l'actualité>"}
        
        Voici les actualités qui te sont fournies pour t'aider à rédiger le post :
    """.trimIndent()


    similarNews.filter { it.minOf { news -> news.releaseDateTime }.toLocalDate() == LocalDate.now() }
        .sortedBy { it.minOf { news -> news.releaseDateTime } }
        .takeLast(5)
        .forEach {
            val date = it.minOf { news -> news.releaseDateTime }

//            println("Similar news detected ($date)\n\n$defaultPrompt\n" + it.joinToString("\n") { news ->
//                "  - ${news.platform} → ${news.title} (${news.link}) : ${news.description}"
//            })
//            println(it.joinToString(" ") { news -> news.image })
            val json = ObjectParser.fromJson(getLLMResponse(httpRequest, defaultPrompt, it).bodyAsText())
            val choiceContent = json["choices"].asJsonArray[0].asJsonObject["message"].asJsonObject["content"].asString
            println(choiceContent)
            val response = ObjectParser.fromJson(choiceContent)
            println(response)
            println()
        }

    httpRequest.close()
}

private suspend fun getLLMResponse(httpRequest: HttpRequest, defaultPrompt: String, similarNews: Set<News>): HttpResponse {
    val body = ObjectParser.toJson(
        mapOf(
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to ""
                ),
                mapOf(
                    "role" to "user",
                    "content" to "$defaultPrompt\n" + similarNews.joinToString("\n") { news ->
                        "  - ${news.platform} → ${news.title} (${news.link}) : ${news.description}"
                    }
                )
            ),
            "model" to "llama-3.1-70b-versatile",
            "temperature" to 0.95,
            "max_tokens" to 500,
            "top_p" to 0.1,
        ))

    return httpRequest.post(
        "https://api.groq.com/openai/v1/chat/completions",
        headers = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ..."
        ),
        body = body
    )
}

fun findIntersectSimilarNews(
    sourceNews: Set<News>,
    otherNews: Set<News>,
    similarNews: MutableSet<Set<News>>,
    intersectSimilarityThreshold: Double,
    newsRange: Long = 1,
) {
    sourceNews.filter { news -> similarNews.none { it.contains(news) } }
        .forEach { sourceNew ->
            val similarities = mutableSetOf(sourceNew)

            otherNews.filter { it != sourceNew && similarNews.none { news -> news.contains(it) } && it.releaseDateTime.isInRange(sourceNew.releaseDateTime.minusDays(newsRange), sourceNew.releaseDateTime.plusDays(newsRange)) }
                .forEach { otherNew ->
                    if (intersectSimilarity(sourceNew, otherNew) > intersectSimilarityThreshold) {
                        similarities.add(otherNew)
                    }
                }

            if (similarities.size > 1)
                similarNews.add(similarities)
        }
}

// For each news, calculate the similarity with each other news
fun intersectSimilarity(news: News, otherNews: News): Double {
    val intersection = news.normalizedDescriptionWords.intersect(otherNews.normalizedDescriptionWords).toMutableSet()
    return intersection.size.toDouble() / (news.normalizedDescriptionWords.size + otherNews.normalizedDescriptionWords.size - intersection.size)
}
