package fr.shikkanime.wrappers.impl

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper

object PrimeVideoWrapper : AbstractPrimeVideoWrapper(){
    override fun getShowVideos(countryCode: CountryCode, showId: String): List<Episode>? {
        val document = loadContent(countryCode, showId)

        val showName = document.selectFirst("h1[data-automation-id=\"title\"]")?.text()
            ?: document.selectFirst("h1[data-testid=\"title-art\"] img")?.attr("alt")
            ?: return null

        val showBanner = document.selectFirst("div[data-automation-id=\"hero-background\"] source[type=\"image/jpeg\"]")
            ?.attr("srcset")?.split(", ")?.maxByOrNull { it.split(" ").last().replace("w", "").toInt() }
            ?.substringBefore(" ") ?: return null

        val showDescription = document.selectFirst(".dv-dp-node-synopsis")?.text()?.normalize()

        return document.select("li[data-testid=\"episode-list-item\"]").map { episode ->
            val episodeSeasonAndNumber = episode.select("span.izvPPq > span").text()
            val season = episodeSeasonAndNumber.substringAfter("S. ").substringBefore(" ").trim().toIntOrNull() ?: 1
            val episodeNumber = episodeSeasonAndNumber.substringAfter("ÉP. ").substringBefore(" ").trim().toIntOrNull() ?: -1
            val episodeTitle = episode.selectFirst("span.P1uAb6")!!.text().normalize()
            val episodeSynopsis = episode.selectFirst("div[dir=\"auto\"]")!!.text().normalize()

            val episodeImage = episode.select("div[data-testid=\"episode-image\"] source")
                .firstOrNull { it.attr("type") == "image/jpeg" || it.attr("type") == "image/png" }

            val episodeImageUrl = episodeImage!!.attr("srcset").split(", ")
                .maxByOrNull { it.split(" ").last().replace("w", "").toInt() }!!.substringBefore(" ")

            val episodeDuration = episode.selectFirst("div[data-testid=\"episode-runtime\"]")!!.text()
                .substringBefore("min").trim().toLongOrNull()?.times(60) ?: -1

            Episode(
                Show(
                    showId,
                    showName,
                    showBanner,
                    showDescription
                ),
                EncryptionManager.toSHA512("$showId-$season-$episodeNumber").substring(0..<8),
                season,
                episodeNumber,
                episodeTitle,
                episodeSynopsis,
                "$BASE_URL/-/${countryCode.name.lowercase()}/detail/$showId",
                episodeImageUrl,
                episodeDuration
            )
        }
    }
}