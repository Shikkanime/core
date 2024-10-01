package fr.shikkanime

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.system.exitProcess

suspend fun main() {
    val countryCode = CountryCode.FR
    val localDate = LocalDate.parse("2021-03-22")
    println(localDate)
    val animationDigitalNetworkPlatform = Constant.injector.getInstance(AnimationDigitalNetworkPlatform::class.java)

    val episodes = AnimationDigitalNetworkWrapper.getLatestVideos(localDate).flatMap {
        try {
            animationDigitalNetworkPlatform.convertEpisode(countryCode, it, ZonedDateTime.now(), needSimulcast = false)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    episodes.groupBy { it.anime + it.releaseDateTime.toLocalDate().toString() }.forEach { (_, animeDayEpisodes) ->
        if (animeDayEpisodes.size > 8) {
            val anime = animeDayEpisodes.first().anime
            checkingNautiljon(anime)
            return@forEach
        }
    }

    exitProcess(0)
}

private fun checkingNautiljon(anime: String) {
    println("Adding all episodes for licence $anime...")
    println("Checking on Nautiljon...")
    // https://www.nautiljon.com/animes/?q=High+School+DxD
    val baseUrl = "https://www.nautiljon.com"

    val nautiljonFinalUrl = "$baseUrl/animes/${URLEncoder.encode(anime.lowercase(), Charset.defaultCharset())}.html"
    val nautiljonDocument = HttpRequest().use { it.getBrowser(nautiljonFinalUrl) }
    val nautiljonError = nautiljonDocument.select("div#erreur").firstOrNull()

    if (nautiljonError != null) {
        println("Error: ${nautiljonError.text()}")
        return
    }

    val nautiljonUrl = "$baseUrl/animes/?q=${URLEncoder.encode(anime, Charset.defaultCharset())}"
    val document = HttpRequest().use { it.getBrowser(nautiljonUrl) }
    val seriesElements = document.select("#content table tr a.eTitre")
    println("Found ${seriesElements.size} results")
    println("Checking series...")
    val seriesPairs = seriesElements.map { it.text() to baseUrl + it.attr("href") }

    val episodes = seriesPairs.flatMap {
        val (seriesName, seriesUrl) = it
        println("Checking $seriesName ($seriesUrl)...")
        val seriesDocument = HttpRequest().use { it.getBrowser(seriesUrl) }

        seriesDocument.select("div#episodes tr a.cboxElement")
            .map { it.text() to baseUrl + it.attr("href") }
    }

    println("Found ${episodes.size} episodes")
    println("Checking episodes to see if an url is equals to ADN...")

    episodes.forEach { (episodeName, episodeUrl) ->
        println("Checking $episodeName ($episodeUrl)...")
        val episodeDocument = HttpRequest().use { it.getBrowser(episodeUrl) }
        val adnUrl = episodeDocument.select("#content > div > div.inline-block.pad5.acenter > a").firstOrNull()?.attr("href")
        println("ADN URL: $adnUrl")
    }
}