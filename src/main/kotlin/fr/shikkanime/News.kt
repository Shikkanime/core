package fr.shikkanime

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.withUTC
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import java.io.File
import java.text.Normalizer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class News(
    val platform: String,
    val slug: String,
    val title: String,
    val releaseDateTime: String,
    val link: String,
    val image: String,
    val description: String,
    val htmlDescription: String? = null,
) {
    override fun toString(): String {
        return "News(platform='$platform', title='$title', releaseDateTime='$releaseDateTime' link='$link', image='$image', description='$description')"
    }
}

suspend fun main() {
    val limit = 20
    val httpRequest = HttpRequest()
    val file = File("news.json")
    val jsonObject = if (file.exists()) ObjectParser.fromJson(file.readText()) else JsonObject()
    val annNews = jsonObject.getAsJsonArray("anime_news_network")
        ?.map { ObjectParser.fromJson(it.asJsonObject.toString(), News::class.java) }?.toMutableSet() ?: mutableSetOf()
    val anNews = jsonObject.getAsJsonArray("adala_news")
        ?.map { ObjectParser.fromJson(it.asJsonObject.toString(), News::class.java) }?.toMutableSet() ?: mutableSetOf()
    val aoNews = jsonObject.getAsJsonArray("animotaku")
        ?.map { ObjectParser.fromJson(it.asJsonObject.toString(), News::class.java) }?.toMutableSet() ?: mutableSetOf()
    val crNews = jsonObject.getAsJsonArray("crunchyroll")
        ?.map { ObjectParser.fromJson(it.asJsonObject.toString(), News::class.java) }?.toMutableSet() ?: mutableSetOf()
    val similarNews = mutableSetOf<Set<News>>()
    val similarityThreshold = 0.175

    val annUrl = "https://www.animenewsnetwork.com"
    val annContent = httpRequest.getBrowser(annUrl, "#mainfeed > div:nth-child(3) > div")

    annContent.getElementsByClass("herald box news t-news preview-only")
        .filter { element ->
            element.dataset()["topics"]?.contains("anime") == true || element.dataset()["topics"]?.contains(
                "manga"
            ) == true
        }
        .take(limit)
        .mapNotNull {
            val title = it.getElementsByTag("h3").text()
            val releaseDateTime = ZonedDateTime.parse(it.getElementsByTag("time").attr("datetime"))
            val link = annUrl + it.getElementsByTag("h3").first()!!.getElementsByTag("a").first()!!.attr("href")
            val split = link.split("/")
            val slug = split[split.size - 2]

            if (annNews.any { news -> news.slug == slug }) {
                return@mapNotNull null
            }

            val image = it.getElementsByClass("thumbnail").first()!!.dataset()["src"]!!
            val descriptionElement = httpRequest.getBrowser(link, "#content-zone")
                .select("#content-zone > div > div.text-zone.easyread-width")

            val description = descriptionElement.text()
            val htmlDescription = descriptionElement.html()

            News(
                platform = "Anime News Network",
                slug = slug,
                title = title,
                releaseDateTime = releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                link = link,
                image = image,
                description = format(description),
                htmlDescription = htmlDescription
            )
        }
        .filter { news -> annNews.none { it.slug == news.slug } }
        .let { annNews.addAll(it) }

    println(annNews.size)
    println(annNews)

    httpRequest.getBrowser(
        "https://adala-news.fr/",
        "#main > div > section.penci-section.penci-dmcheck.penci-elbg-activate.penci-enSticky.penci-structure-22.elementor-section.elementor-top-section.elementor-element.elementor-element-769a74a.elementor-section-boxed.elementor-section-height-default.elementor-section-height-default > div > div.penci-ercol-66.penci-ercol-order-1.penci-sticky-ct.elementor-column.elementor-col-66.elementor-top-column.elementor-element.elementor-element-e18d914 > div > div.elementor-element.elementor-element-ace3bd1.elementor-widget.elementor-widget-penci-latest-posts"
    ).getElementsByClass("list-post penci-item-listp")
        .filter { element -> element.getElementsByTag("span").first()!!.text().contains("ANIME") }
        .take(limit)
        .mapNotNull {
            val title = it.getElementsByTag("h2").text()
            val releaseDateTime = ZonedDateTime.parse(it.getElementsByTag("time").attr("datetime"))
            val link =
                it.getElementsByTag("h2").first()!!.getElementsByTag("a").first()!!.attr("href").removeSuffix("/")
            val slug = link.substringAfterLast("/")

            if (anNews.any { news -> news.slug == slug }) {
                return@mapNotNull null
            }

            val image =
                it.getElementsByClass("thumbnail").first()!!.getElementsByTag("a").first()!!.dataset()["bgset"]!!
            val descriptionElement =
                httpRequest.getBrowser(link, "#penci-post-entry-inner").select("#penci-post-entry-inner")
            val description = descriptionElement.text()
            val htmlDescription = descriptionElement.html()

            News(
                platform = "Adala News",
                slug = slug,
                title = title,
                releaseDateTime = releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                link = link,
                image = image,
                description = format(description),
                htmlDescription = htmlDescription
            )
        }
        .filter { news -> anNews.none { it.slug == news.slug } }
        .let { anNews.addAll(it) }

    println(anNews.size)
    println(anNews)

    ObjectParser.fromJson(
        httpRequest.get("https://animotaku.fr/wp-json/wp/v2/posts?per_page=$limit&categories=26").bodyAsText(),
        JsonArray::class.java
    ).mapNotNull {
        val element = it.asJsonObject
        val title = element["title"].asJsonObject["rendered"].asString
        val releaseDateTime = ZonedDateTime.parse(element["date_gmt"].asString + "Z")
        val link = element["link"].asString.removeSuffix("/")
        val slug = link.substringAfterLast("/")

        if (aoNews.any { news -> news.slug == slug }) {
            return@mapNotNull null
        }

        val image = element["yoast_head_json"].asJsonObject["og_image"].asJsonArray[0].asJsonObject["url"].asString
        val descriptionElement = Jsoup.parse(element["content"].asJsonObject["rendered"].asString)
        val description = descriptionElement.text()
        val htmlDescription = descriptionElement.html()

        News(
            platform = "AnimOtaku",
            slug = slug,
            title = title,
            releaseDateTime = releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            link = link,
            image = image,
            description = format(description),
            htmlDescription = htmlDescription
        )
    }
        .filter { news -> aoNews.none { it.slug == news.slug } }
        .let { aoNews.addAll(it) }

    println(aoNews.size)
    println(aoNews)

    val crContent = httpRequest.get("https://cr-news-api-service.prd.crunchyrollsvc.com/v1/fr-FR/rss").bodyAsText()
        .replace(System.lineSeparator(), "").replace("\n", "")

    "<item>(.*?)</item>".toRegex()
        .findAll(crContent)
        .map { it.value }
        .toList()
        .take(limit)
        .mapNotNull {
            val xml = ObjectParser.fromXml(it, JsonObject::class.java)

            val title = xml["title"].asString
            val releaseDateTime = ZonedDateTime.parse(xml["pubDate"].asString, DateTimeFormatter.RFC_1123_DATE_TIME)
            val link = xml["link"].asString
            val slug = link.substringAfterLast("/")

            if (crNews.any { news -> news.slug == slug }) {
                return@mapNotNull null
            }

            val image = xml["media:thumbnail"].asJsonObject["url"].asString
            val description = xml["content:encoded"].asString

            News(
                platform = "Crunchyroll",
                slug = slug,
                title = title,
                releaseDateTime = releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                link = link,
                image = image,
                description = format(description),
            )
        }
        .filter { news -> crNews.none { it.slug == news.slug } }
        .let { crNews.addAll(it) }

    println(crNews.size)
    println(crNews)

    httpRequest.close()

    ObjectParser.toJson(
        mapOf(
            "anime_news_network" to annNews,
            "adala_news" to anNews,
            "animotaku" to aoNews,
            "crunchyroll" to crNews,
        )
    ).let {
        file.writeText(it)
    }

    findSimilarNews(annNews, anNews + aoNews + crNews, similarNews, similarityThreshold)
    findSimilarNews(anNews, annNews + aoNews + crNews, similarNews, similarityThreshold)
    findSimilarNews(aoNews, annNews + anNews + crNews, similarNews, similarityThreshold)
    findSimilarNews(crNews, annNews + anNews + aoNews, similarNews, similarityThreshold)

    println()

    val prompt = """
        Condense les actualités suivantes en une seule. Elles portent sur le même sujet. L'actualité résumée sera publiée sur mon site, donc rend la pertinente et la plus longue possible sans inventer.
        Sers-toi uniquement des informations qui te sont fournies. Écrit là de manière formelle.

        Voici les informations qui te sont fournies :

    """.trimIndent()

    similarNews
        .sortedByDescending {
            // Average release date
            it.map { news -> ZonedDateTime.parse(news.releaseDateTime).toInstant().toEpochMilli() }
                .average()
        }
        .forEach {
        println("$prompt\n" + it.joinToString("\n") { news ->
            val index = it.indexOf(news)
            "#${index + 1}: ${news.platform} → ${news.title} (${news.link})\n${news.description}"
        })

        println()
        println("#".repeat(100))
        println()
    }
}

fun findSimilarNews(
    sourceNews: Set<News>,
    otherNews: Set<News>,
    similarNews: MutableSet<Set<News>>,
    similarityThreshold: Double
) {
    sourceNews.forEach { sourceNew ->
        if (similarNews.any { it.contains(sourceNew) }) {
            return@forEach
        }

        val on = otherNews.filter { it != sourceNew && !similarNews.any { news -> news.contains(it) } }
        val similarities = mutableSetOf(sourceNew)

        on.forEach { otherNew ->
            val similarity = similarity(sourceNew, otherNew)

            if (similarity > similarityThreshold) {
                similarities.add(otherNew)
            }
        }

        if (similarNews.none { it.containsAll(similarities) } && similarities.size > 1) {
            similarNews.add(similarities)
        }
    }
}

fun CharSequence.unaccented(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
}

// For each news, calculate the similarity with each other news
fun similarity(news: News, otherNews: News): Double {
    val delemiters = arrayOf(" ", ".", ",", "!", "?", ":", ";", "(", ")", "[", "]", "{", "}", "<", ">")
    val words = news.description.split(*delemiters).map { it.lowercase().unaccented() }.toSet()
    val otherWords = otherNews.description.split(*delemiters).map { it.lowercase().unaccented() }.toSet()
    val intersection = words.intersect(otherWords)
    return intersection.size.toDouble() / (words.size + otherWords.size - intersection.size)
}

private fun format(text: String): String {
    val trim = text.replace("\n".toRegex(), " ").replace(System.lineSeparator().toRegex(), " ").trim { it <= ' ' }

    return if (trim.contains("Source")) {
        trim.substring(0, trim.indexOf("Source"))
    } else {
        trim
    }
}
