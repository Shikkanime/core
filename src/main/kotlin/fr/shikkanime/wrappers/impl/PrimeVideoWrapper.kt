package fr.shikkanime.wrappers.impl

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper

object PrimeVideoWrapper : AbstractPrimeVideoWrapper(){
    override fun getShowVideos(countryCode: CountryCode, showId: String): List<Episode>? {
        val document = loadContent(countryCode, showId)
        val showName = (document.selectFirst("h1[data-automation-id=\"title\"]")?.text() ?: document.selectFirst("img.ljcPsM")?.attr("alt")).takeIf { it.isNullOrBlank().not() } ?: return null
        val episodes = document.select("li.c5qQpO")

        return episodes.map { episode ->
            val episodeSeasonAndNumber = episode.select("span.izvPPq > span").text()
            val season = episodeSeasonAndNumber.substringAfter("S. ").substringBefore(" ").trim().toIntOrNull() ?: 1
            val episodeNumber = episodeSeasonAndNumber.substringAfter("ÉP. ").substringBefore(" ").trim().toIntOrNull() ?: -1

            Episode(
                Show(
                    showId,
                    showName,
                    document.selectFirst("source[type=\"image/jpeg\"]")!!.attr("srcset").split(", ").maxBy { it.split(" ").last().replace("w", "").toInt() }.substringBefore(" "),
                    document.selectFirst(".dv-dp-node-synopsis")!!.text().normalize()
                ),
                EncryptionManager.toSHA512("$showId-$season-$episodeNumber").substring(0..<8),
                season,
                episodeNumber,
                episode.selectFirst("span.P1uAb6")!!.text().normalize(),
                episode.selectFirst("div[dir=\"auto\"]")!!.text().normalize(),
                "$BASE_URL/-/${countryCode.name.lowercase()}/detail/$showId",
                episode.selectFirst("source[type=\"image/jpeg\"]")!!.attr("srcset").split(", ").maxBy { it.split(" ").last().replace("w", "").toInt() }.substringBefore(" "),
                episode.selectFirst("div[data-testid=\"episode-runtime\"]")!!.text().substringBefore("min").trim().toLongOrNull()?.times(60) ?: -1
            )
        }
    }
}