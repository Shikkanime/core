package fr.shikkanime.wrappers.factories

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.time.Duration

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
    protected val configService: ConfigService by lazy { Constant.injector.getInstance(ConfigService::class.java) }

    @Synchronized
    private fun getAccessToken() = MapCache.getOrCompute(
        "AbstractDisneyPlusWrapper.getAccessToken",
        duration = Duration.ofHours(2),
        classes = listOf(Config::class.java),
        typeToken = object : TypeToken<MapCacheValue<String>>() {},
        key = StringUtils.EMPTY_STRING
    ) {
        runBlocking {
            val refreshTokenConfig = configService.findByName(ConfigPropertyKey.DISNEY_PLUS_REFRESH_TOKEN.name)
            if (refreshTokenConfig == null || refreshTokenConfig.propertyValue.isNullOrBlank())
                throw IllegalStateException("Disney Plus refresh token is not set")

            val response = HttpRequest.post(
                "https://login.disney.com/token",
                headers = mapOf(
                    HttpHeaders.ContentType to ContentType.Application.FormUrlEncoded.toString(),
                    "X-Dss-Client-Api-Key" to (configCacheService.getValueAsString(ConfigPropertyKey.DISNEY_PLUS_AUTHORIZATION) ?: StringUtils.EMPTY_STRING)
                ),
                body = "grant_type=refresh_token&client_id=DTCI-DISNEYPLUS.WEB&scope=openid streaming api:identity:user.write&refresh_token=${refreshTokenConfig.propertyValue}"
            )
            require(response.status == HttpStatusCode.OK) { "Failed to get access token (${response.status.value})" }
            val json = ObjectParser.fromJson(response.bodyAsText())

            // Change refresh token
            refreshTokenConfig.propertyValue = requireNotNull(json.getAsString("refresh_token")) { "Refresh token is null or empty"}
            configService.update(refreshTokenConfig)

            requireNotNull(json.getAsString("access_token")) { "Access token is null or empty"}
        }
    }

    protected suspend fun HttpRequest.getWithAccessToken(url: String) = get(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAccessToken()}"))
    protected suspend fun HttpRequest.postWithAccessToken(url: String, headers: Map<String, String>, body: String) = post(url, headers = mapOf(HttpHeaders.Authorization to "Bearer ${getAccessToken()}").plus(headers), body = body)

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