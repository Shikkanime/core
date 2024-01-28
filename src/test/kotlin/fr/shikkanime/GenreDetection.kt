package fr.shikkanime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeRecommendationDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.Genre
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import java.net.URLEncoder

fun main() {
    // https://www.nautiljon.com/animes/?q=Solo+Leveling
    val baseUrl = "https://www.nautiljon.com"
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val animes = animeService.findAll().filter { it.genres.isEmpty() }
    val httpRequest = HttpRequest()
    val nautiljonPages = mutableMapOf<Anime, String>()

    animes.forEach {
        val dto = AbstractConverter.convert(it, AnimeRecommendationDto::class.java)
        val searchResponse = httpRequest.getBrowser("$baseUrl/animes/?q=${URLEncoder.encode(dto.shortName, "UTF-8")}")
        val titles = searchResponse.select(".eTitre").map { a -> a.text() to a.attr("href") }
        val infos = searchResponse.select(".infos_small").map { a -> a.text().replace("(", "").replace(")", "") }
        // Group titles and infos
        val titlesAndInfos = if (infos.isNotEmpty())
            titles.zip(infos).map { pair -> pair.first to pair.second }
        else
            titles.map { pair -> pair to "" }
        println(titlesAndInfos)

        var found = false
        var url = ""

        if (titlesAndInfos.size == 1) {
            url = "$baseUrl${titlesAndInfos.first().first.second}"
            nautiljonPages[it] = url
            found = true
        } else {
            titlesAndInfos.forEach { pair ->
                val title = pair.first.first
                val info = pair.second

                if ((title.equals(it.name, true) ||
                    info.equals(it.name, true)) ||
                    (title.equals(dto.shortName, true) ||
                    info.equals(dto.shortName, true))) {
                    url = "$baseUrl${pair.first.second}"
                    nautiljonPages[it] = url
                    found = true
                }
            }
        }

        if (!found) {
            println("Not found: ${it.name}")
            return@forEach
        }

        val detailsResponse = httpRequest.getBrowser(url)
        val genresS = detailsResponse.select("span[itemprop=genre]").map { a -> a.text() }.toSet()
        val genresString = genresS.joinToString(", ")
        println("${it.name} -> $genresString")
        val genres = genresS.mapNotNull { genre -> Genre.from(genre) }.toMutableSet()
        println(genres)

        it.genres.addAll(genres)
        animeService.update(it)
        Thread.sleep(1000)
    }

    httpRequest.close()
}
