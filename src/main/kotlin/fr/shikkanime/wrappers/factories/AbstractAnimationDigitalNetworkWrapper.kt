package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import java.io.Serializable
import java.time.LocalDate
import java.time.ZonedDateTime

abstract class AbstractAnimationDigitalNetworkWrapper : IStreamingPlatformWrapper<Int, AbstractAnimationDigitalNetworkWrapper.Show, AbstractAnimationDigitalNetworkWrapper.Episode> {
    companion object {
        private val sizeRegex = "\\d+x\\d+".toRegex()
        private val licenceSizeRegex = "\\d+x\\d+".toRegex()
        private val epsRegex = "/eps$".toRegex()
        private val afficheRegex = "/portrait-with-logo$".toRegex()
        private val licenseRegex = "/landscape-with-logo(\\..*)?$".toRegex()
    }

    data class Microdata(
        val startDate: ZonedDateTime,
    ) : Serializable

    data class Show(
        override val id: Int,
        val shortTitle: String?,
        val title: String,
        val originalTitle: String?,
        val image2x: String,
        val imageHorizontal2x: String,
        val summary: String?,
        val genres: List<String> = emptyList(),
        val simulcast: Boolean,
        val firstReleaseYear: String,
        val microdata: Microdata? = null,
    ) : Serializable, IStreamingPlatformWrapper.Id<Int> {
        val fullHDImage: String
            get() = image2x.replace(sizeRegex, "1560x2340")
                .replace(afficheRegex, "/portrait-with-logo.width=1080,height=1920,quality=100")
        val fullHDBanner: String
            get() = imageHorizontal2x.replace(sizeRegex, "1920x1080")
                .replace(licenseRegex, "/landscape-with-logo.width=1920,height=1080,quality=100")
        val fullHDCarousel: String
            get() = imageHorizontal2x.replace("license_", "carousel169_")
                .replace(licenceSizeRegex, "1920x1080")
                .replace(licenseRegex, "/landscape.width=1920,height=1080,quality=100")
        val fullHDTitle: String
            get() = imageHorizontal2x.replace("license_", "title_")
                .replace(licenceSizeRegex, "1920x1080")
                .replace(licenseRegex, "/logo.width=1920,quality=100")
    }

    data class Episode(
        override val id: Int,
        val title: String,
        val season: String?,
        var releaseDate: ZonedDateTime?,
        val shortNumber: String?,
        val type: String,
        val name: String?,
        val summary: String?,
        val image2x: String,
        val url: String,
        val duration: Long,
        val languages: List<String> = emptyList(),
        val show: Show,
    ) : Serializable, IStreamingPlatformWrapper.Id<Int> {
        val fullHDImage: String
            get() = image2x.replace(sizeRegex, "1920x1080")
                .replace(epsRegex, "/eps.width=1920,height=1080,quality=100")
    }

    protected val baseUrl = "https://gw.api.animationdigitalnetwork.com/"

    protected suspend fun HttpRequest.getWithHeaders(locale: String, url: String) = get(url, headers = mapOf("X-Source" to "Web", "X-Target-Distribution" to locale.split("-").first().lowercase()))

    abstract suspend fun getLatestEpisodes(locale: String, date: LocalDate): Array<Episode>
    abstract suspend fun getEpisode(locale: String, id: Int): Episode
}