package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest
import java.io.Serializable
import java.time.LocalDate
import java.time.ZonedDateTime

abstract class AbstractAnimationDigitalNetworkWrapper {
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
        val id: Int,
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
    ) : Serializable {
        val fullHDImage: String
            get() = image2x.replace(sizeRegex, "1560x2340")
                .replace(afficheRegex, "/portrait-with-logo.width=1560,height=2340,quality=100")
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

    data class Video(
        val id: Int,
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
    ) : Serializable {
        val fullHDImage: String
            get() = image2x.replace(sizeRegex, "1920x1080")
                .replace(epsRegex, "/eps.width=1920,height=1080,quality=100")
    }

    protected val baseUrl = "https://gw.api.animationdigitalnetwork.com/"
    protected val httpRequest = HttpRequest()

    protected suspend fun HttpRequest.getWithHeaders(countryCode: CountryCode, url: String) = getWithHeaders(countryCode.name, url)
    protected suspend fun HttpRequest.getWithHeaders(country: String, url: String) = get(url, headers = mapOf("X-Source" to "Web", "X-Target-Distribution" to country.lowercase()))

    abstract suspend fun getLatestVideos(countryCode: CountryCode, date: LocalDate): Array<Video>
    abstract suspend fun getShow(country: String, id: Int): Show
    abstract suspend fun getShowVideos(countryCode: CountryCode, id: Int): Array<Video>
    abstract suspend fun getVideo(countryCode: CountryCode, id: Int): Video

    suspend fun getPreviousVideo(countryCode: CountryCode, showId: Int, videoId: Int): Video? {
        val videos = getShowVideos(countryCode, showId)
        val videoIndex = videos.indexOfFirst { it.id == videoId }
        require(videoIndex != -1) { "Video not found" }
        return if (videoIndex == 0) null else videos[videoIndex - 1]
    }

    suspend fun getNextVideo(countryCode: CountryCode, showId: Int, videoId: Int): Video? {
        val videos = getShowVideos(countryCode, showId)
        val videoIndex = videos.indexOfFirst { it.id == videoId }
        require(videoIndex != -1) { "Video not found" }
        return if (videoIndex == videos.size - 1) null else videos[videoIndex + 1]
    }
}