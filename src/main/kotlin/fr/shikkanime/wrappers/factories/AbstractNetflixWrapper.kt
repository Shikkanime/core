package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest
import org.jsoup.nodes.Document

abstract class AbstractNetflixWrapper {
    data class Show(
        val id: String,
        val name: String,
        val banner: String,
        val description: String?,
    )

    data class Episode(
        val show: Show,
        val id: String,
        val season: Int,
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
    )

    private val maxRetry = 3

    internal fun loadContent(showId: String, countryCode: CountryCode, i: Int = 1): Document? {
        if (i >= maxRetry) {
            return null
        }

        val document = HttpRequest(
            countryCode,
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)" // Force Googlebot user-agent to bypass A-B testing
        ).use { it.getBrowser("$BASE_URL${countryCode.name.lowercase()}/title/$showId") }

        if (checkLanguage && document.getElementsByTag("html").attr("lang") != countryCode.name.lowercase()) {
            return loadContent(showId, countryCode, i + 1)
        }

        // Is new design?
        if (document.selectXpath("//*[@id=\"appMountPoint\"]/div/div[2]/div/header").isNotEmpty()) {
            return loadContent(showId, countryCode, i + 1)
        }

        return document
    }

    abstract fun getShowVideos(countryCode: CountryCode, showId: String, seasonName: String = "", season: Int = 1): List<Episode>?

    companion object {
        const val BASE_URL = "https://www.netflix.com/"
        // Only for testing
        var checkLanguage = true
    }
}