package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.HttpRequest

object PrimeVideoWrapper {
    fun getShowVideos(countryCode: String, locale: String, id: String): List<JsonObject> {
        val document =
            HttpRequest().use { it.getBrowser("https://www.primevideo.com/-/${countryCode.lowercase()}/detail/$id?language=$locale") }
        val showName = document.selectFirst("h1[data-automation-id=\"title\"]")!!.text()
        val showBanner = document.selectFirst("img[data-testid=\"base-image\"]")!!.attr("src")
        val showDescription = document.selectFirst(".dv-dp-node-synopsis")!!.text()
        val domEpisodes = document.selectFirst("ol.GG33WY")!!.select("li.c5qQpO")

        return domEpisodes.mapNotNull { domEpisode ->
            val episodeTitle = domEpisode.selectFirst("span.P1uAb6")!!.text()
            val episodeSeasonAndNumber = domEpisode.selectFirst("span.izvPPq > span")!!.text()
            val season = episodeSeasonAndNumber.substringAfter("S. ").substringBefore(" ").trim().toIntOrNull() ?: 1
            val episodeNumber =
                episodeSeasonAndNumber.substringAfter("ÉP. ").substringBefore(" ").trim().toIntOrNull() ?: -1
            val duration = domEpisode.selectFirst("div[data-testid=\"episode-runtime\"]")!!.text()
            val durationInSeconds = duration.substringBefore("min").trim().toLongOrNull()?.times(60) ?: -1
            val image = domEpisode.selectFirst("img[data-testid=\"base-image\"]")!!.attr("src")
            val episodeDescription = domEpisode.selectFirst("div[dir=\"auto\"]")!!.text()

            JsonObject().apply {
                add("show", JsonObject().apply {
                    addProperty("name", showName)
                    addProperty("banner", showBanner)
                    addProperty("description", showDescription)
                })
                addProperty(
                    "id",
                    EncryptionManager.toSHA512("$id-${season}-$episodeNumber").substring(0..<8)
                )
                addProperty("season", season)
                addProperty("number", episodeNumber)
                addProperty("title", episodeTitle)
                addProperty("url", "https://www.primevideo.com/-/${countryCode.lowercase()}/detail/$id")
                addProperty("image", image)
                addProperty("duration", durationInSeconds)
                addProperty("description", episodeDescription)
            }
        }
    }
}