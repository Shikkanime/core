package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest

abstract class AbstractPrimeVideoWrapper {
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


    internal fun loadContent(countryCode: CountryCode, showId: String) = HttpRequest(
        countryCode,
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
    ).use { it.getWithBrowser("$BASE_URL/-/${countryCode.name.lowercase()}/detail/$showId?language=${countryCode.locale}") }

    abstract fun getShowVideos(countryCode: CountryCode, showId: String): List<Episode>?

    companion object {
        const val BASE_URL = "https://www.primevideo.com"
    }
}