package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import io.ktor.http.*
import java.io.Serializable

abstract class AbstractDisneyPlusWrapper : IStreamingPlatformWrapper<String, AbstractDisneyPlusWrapper.Show, AbstractDisneyPlusWrapper.Episode> {
    data class Show(
        override val id: String,
        val name: String,
        val image: String,
        val banner: String,
        val carousel: String,
        val title: String,
        val description: String?,
        val seasons: Set<String>
    ) : Serializable, IStreamingPlatformWrapper.Id<String>

    data class Episode(
        val show: Show,
        override val id: String,
        val oldId: String?,
        val seasonId: String,
        val season: Int,
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
        val resourceId: String,
        val audioLocales: Array<String>
    ) : Serializable, IStreamingPlatformWrapper.Id<String> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Episode) return false

            return season == other.season &&
                    number == other.number &&
                    duration == other.duration &&
                    show == other.show &&
                    id == other.id &&
                    oldId == other.oldId &&
                    seasonId == other.seasonId &&
                    title == other.title &&
                    description == other.description &&
                    url == other.url &&
                    image == other.image &&
                    resourceId == other.resourceId &&
                    audioLocales.contentEquals(other.audioLocales)
        }

        override fun hashCode(): Int {
            var result = season
            result = 31 * result + number
            result = 31 * result + duration.hashCode()
            result = 31 * result + show.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + oldId.hashCode()
            result = 31 * result + seasonId.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + image.hashCode()
            result = 31 * result + resourceId.hashCode()
            result = 31 * result + audioLocales.contentHashCode()
            return result
        }
    }

    data class Metadata(
        val showId: String,
        val id: String,
    ) : Serializable

    protected val baseUrl = "https://disney.api.edge.bamgrid.com/"
    protected val configCacheService: ConfigCacheService by lazy { Constant.injector.getInstance(ConfigCacheService::class.java) }

    protected suspend fun HttpRequest.getWithAccessToken(url: String) = get(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_ACCESS_TOKEN)}"))
    protected suspend fun HttpRequest.postWithAccessToken(url: String, headers: Map<String, String>, body: String) = post(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_ACCESS_TOKEN)}").plus(headers), body = body)

    abstract suspend fun getLatestShowIds(): Array<String>

    override suspend fun getEpisodesByShowId(locale: String, showId: String) = getEpisodesByShowId(
        locale,
        showId,
        configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
    )

    abstract suspend fun getEpisodesByShowId(locale: String, showId: String, checkAudioLocales: Boolean): Array<Episode>

    abstract suspend fun getMetadataByEpisodeId(episodeId: String): Metadata

    fun getImageUrl(id: String) = "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/$id/compose"
}