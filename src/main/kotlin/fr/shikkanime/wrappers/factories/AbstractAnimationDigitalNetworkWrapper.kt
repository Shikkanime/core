package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import java.io.Serializable
import java.time.LocalDate
import java.time.ZonedDateTime

abstract class AbstractAnimationDigitalNetworkWrapper {
    companion object {
        private val sizeRegex = "\\d+x\\d+".toRegex()
        private val licenceSizeRegex = "\\d+x\\d+".toRegex()
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
        val fullHDBanner: String
            get() = imageHorizontal2x.replace(sizeRegex, "1920x1080")
        val fullHDCarousel: String
            get() = imageHorizontal2x.replace("license_", "carousel169_").replace(licenceSizeRegex, "1920x1080")
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
    }

    protected val baseUrl = "https://gw.api.animationdigitalnetwork.com/"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getLatestVideos(date: LocalDate): Array<Video>
    abstract suspend fun getShow(id: Int): Show
    abstract suspend fun getShowVideos(id: Int): Array<Video>
    abstract suspend fun getVideo(id: Int): Video

    suspend fun getPreviousVideo(showId: Int, videoId: Int): Video? {
        val videos = getShowVideos(showId)
        val videoIndex = videos.indexOfFirst { it.id == videoId }
        require(videoIndex != -1) { "Video not found" }
        return if (videoIndex == 0) null else videos[videoIndex - 1]
    }

    suspend fun getNextVideo(showId: Int, videoId: Int): Video? {
        val videos = getShowVideos(showId)
        val videoIndex = videos.indexOfFirst { it.id == videoId }
        require(videoIndex != -1) { "Video not found" }
        return if (videoIndex == videos.size - 1) null else videos[videoIndex + 1]
    }
}