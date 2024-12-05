package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.normalize
import org.jsoup.nodes.Document

object NetflixWrapper {
    private val logger =  LoggerFactory.getLogger(javaClass)
    private const val MAX_RETRY = 5
    private const val BASE_URL = "https://www.netflix.com"

    private fun loadContent(id: String, countryCode: CountryCode, i: Int = 0): Document? {
        if (i >= MAX_RETRY) {
            logger.severe("Failed to fetch Netflix page for $id in ${countryCode.name}")
            return null
        }

        val document = HttpRequest(
            countryCode,
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
        ).use { it.getBrowser("$BASE_URL/${countryCode.name.lowercase()}/title/$id") }

        if (document.getElementsByTag("html").attr("lang") != countryCode.name.lowercase()) {
            logger.warning("Failed to fetch Netflix page for $id in ${countryCode.name}, retrying... (${i + 1}/${MAX_RETRY})")
            return loadContent(id, countryCode, i + 1)
        }

        // Is new design?
        if (document.selectXpath("//*[@id=\"appMountPoint\"]/div/div[2]/div/header").isNotEmpty()) {
            logger.warning("New Netflix design detected, trying to get the old design... (${i + 1}/${MAX_RETRY})")
            return loadContent(id, countryCode, i + 1)
        }

        return document
    }

    fun getShowVideos(countryCode: CountryCode, id: String, seasonName: String = "", season: Int): List<JsonObject>? {
        val document = loadContent(id, countryCode) ?: return null
        val seasonsOption = document.select("#season-selector-container").select("option")
        val seasons = document.select(".season")

        // Group episodes by season
        val episodesBySeason = seasons.mapIndexed { index, seasonElement ->
            val seasonName = seasonsOption.getOrNull(index)?.text()
            val seasonEpisodes = seasonElement.select("li.episode")
            seasonName to seasonEpisodes
        }.toMap()

        var useAllSeasons = false

        val episodes = episodesBySeason[seasonName] ?: run {
            logger.warning("Season name is blank or not found, fetching all episodes from all seasons and ignoring season number")
            useAllSeasons = true
            document.select("li.episode")
        }

        return episodes.mapIndexedNotNull { index, episode ->
            val episodeTitleAndNumber = episode.selectFirst(".episode-title")?.text()
            val episodeNumber = episodeTitleAndNumber?.substringBefore(".")?.toIntOrNull() ?: (index + 1)
            val episodeSeason = if (useAllSeasons) seasons.indexOf(episode.closest(".season")!!) + 1 else season

            JsonObject().apply {
                add("show", JsonObject().apply {
                    addProperty("name", document.selectFirst(".title-title")!!.text())
                    addProperty("banner", document.selectXpath("//*[@id=\"section-hero\"]/div[1]/div[2]/picture/source[2]").attr("srcset").substringBefore("?"))
                    addProperty("description", document.selectFirst(".title-info-synopsis")!!.text().normalize())
                })
                addProperty(
                    "id",
                    EncryptionManager.toSHA512("$id-$episodeSeason-$episodeNumber").substring(0..<8)
                )
                addProperty("season", episodeSeason)
                addProperty("number", episodeNumber)
                addProperty("title", episodeTitleAndNumber?.substringAfter(".").normalize())
                addProperty("url", "$BASE_URL/${countryCode.name.lowercase()}/title/$id")
                addProperty("image", episode.selectFirst(".episode-thumbnail-image")!!.attr("src").substringBefore("?"))
                addProperty("duration", episode.selectFirst(".episode-runtime")?.text()?.substringBefore(" ")?.trim()?.toLongOrNull()?.times(60) ?: -1)
                addProperty("description", episode.selectFirst(".epsiode-synopsis")?.text())
            }
        }
    }
}