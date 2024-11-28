package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.normalize

object PrimeVideoWrapper {
    private const val BASE_URL = "https://www.primevideo.com"

    fun getShowVideos(countryCode: CountryCode, locale: String, id: String): List<JsonObject> {
        val document = HttpRequest(countryCode).use { it.getBrowser("$BASE_URL/-/${countryCode.name.lowercase()}/detail/$id?language=$locale") }
        val domEpisodes = document.selectFirst("ol.SIGk7D")?.select("li.c5qQpO") ?: return emptyList()

        return domEpisodes.mapNotNull { domEpisode ->
            val episodeSeasonAndNumber = domEpisode.selectFirst("span.izvPPq > span")!!.text()
            val season = episodeSeasonAndNumber.substringAfter("S. ").substringBefore(" ").trim().toIntOrNull() ?: 1
            val episodeNumber = episodeSeasonAndNumber.substringAfter("ÉP. ").substringBefore(" ").trim().toIntOrNull() ?: -1

            JsonObject().apply {
                add("show", JsonObject().apply {
                    addProperty("name", document.selectFirst("h1[data-automation-id=\"title\"]")!!.text())
                    addProperty("banner", document.selectFirst("img[data-testid=\"base-image\"]")!!.attr("src"))
                    addProperty("description", document.selectFirst(".dv-dp-node-synopsis")!!.text().normalize())
                })
                addProperty(
                    "id",
                    EncryptionManager.toSHA512("$id-$season-$episodeNumber").substring(0..<8)
                )
                addProperty("season", season)
                addProperty("number", episodeNumber)
                addProperty("title", domEpisode.selectFirst("span.P1uAb6")!!.text().normalize())
                addProperty("url", "$BASE_URL/-/${countryCode.name.lowercase()}/detail/$id")
                addProperty("image", domEpisode.selectFirst("img[data-testid=\"base-image\"]")!!.attr("src"))
                addProperty("duration", domEpisode.selectFirst("div[data-testid=\"episode-runtime\"]")!!.text().substringBefore("min").trim().toLongOrNull()?.times(60) ?: -1)
                addProperty("description", domEpisode.selectFirst("div[dir=\"auto\"]")!!.text().normalize())
            }
        }
    }
}