package fr.shikkanime.wrappers.impl

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper

object NetflixWrapper : AbstractNetflixWrapper(){
    override fun getShowVideos(countryCode: CountryCode, showId: String, seasonName: String, season: Int): List<Episode>? {
        val document = loadContent(countryCode, showId) ?: return null
        val seasonsOption = document.select("#season-selector-container").select("option")
        val seasons = document.select(".season")

        // Group episodes by season
        val episodesBySeason = seasons.mapIndexed { index, seasonElement ->
            seasonsOption.getOrNull(index)?.text() to seasonElement.select("li.episode")
        }.toMap()

        var useAllSeasons = false

        val episodes = episodesBySeason[seasonName] ?: run {
            useAllSeasons = true
            document.select("li.episode")
        }

        return episodes.mapIndexedNotNull { index, episode ->
            val episodeTitleAndNumber = episode.selectFirst(".episode-title")?.text()
            val episodeNumber = episodeTitleAndNumber?.substringBefore(".")?.toIntOrNull() ?: (index + 1)
            val episodeSeason = if (useAllSeasons) seasons.indexOf(episode.closest(".season")!!) + 1 else season

            Episode(
                Show(
                    showId,
                    document.selectFirst(".title-title")!!.text(),
                    document.selectXpath("//*[@id=\"section-hero\"]/div[1]/div[2]/picture/source[2]").attr("srcset").substringBefore("?"),
                    document.selectFirst(".title-info-synopsis")?.text().normalize()
                ),
                EncryptionManager.toSHA512("$showId-$episodeSeason-$episodeNumber").substring(0..<8),
                episodeSeason,
                episodeNumber,
                episodeTitleAndNumber?.substringAfter(".").normalize(),
                episode.selectFirst(".epsiode-synopsis")?.text(),
                "$BASE_URL${countryCode.name.lowercase()}/title/$showId",
                episode.selectFirst(".episode-thumbnail-image")!!.attr("src").substringBefore("?"),
                episode.selectFirst(".episode-runtime")?.text()?.substringBefore(" ")?.trim()?.toLongOrNull()?.times(60) ?: -1
            )
        }
    }
}