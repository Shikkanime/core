package fr.shikkanime.jobs

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

private const val IMAGE_NULL_ERROR = "Image is null"

class FetchDeprecatedEpisodeJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var episodeService: EpisodeService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun run() {

        val takeSize = configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODE_DESCRIPTION_SIZE, 0)
        val now = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val deprecatedDateTime = now.minusDays(
            configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_DEPRECATED_EPISODE_DATE, 30).toLong()
        )
        val crunchyrollEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.CRUN, deprecatedDateTime)
        val adnEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.ANIM, deprecatedDateTime)
        val episodes = (crunchyrollEpisodes + adnEpisodes).shuffled().take(takeSize)

        logger.info("Found ${episodes.size} episodes")

        if (episodes.isEmpty()) {
            return
        }

        val httpRequest = HttpRequest()
        val anonymousAccessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(anonymousAccessToken) }
        var count = 0

        episodes.forEachIndexed { index, episode ->
            logger.info("Fetching episode description ${index + 1}/${episodes.size}")

            if (update(episode, httpRequest, anonymousAccessToken, cms, now)) {
                count++
            }
        }

        httpRequest.close()

        if (count <= 0) {
            return
        }

        logger.info("Updated $count episodes")
        MapCache.invalidate(Episode::class.java)
    }

    private fun update(
        episode: Episode,
        httpRequest: HttpRequest,
        anonymousAccessToken: String,
        cms: CrunchyrollWrapper.CMS,
        now: ZonedDateTime,
    ): Boolean {
        var needUpdate = false

        val s = "${episode.anime?.name} - S${episode.season} ${
            when (episode.episodeType!!) {
                EpisodeType.EPISODE -> "EP"
                EpisodeType.SPECIAL -> "SP"
                EpisodeType.FILM -> "MOV"
            }
        }${episode.number}"

        try {
            val content =
                runBlocking { normalizeContent(episode, httpRequest, anonymousAccessToken, cms) } ?: return false
            val title = normalizeTitle(episode.platform!!, content)
            val description = normalizeDescription(episode.platform!!, content)
            val image = normalizeImage(episode.platform!!, content)
            logger.config("$s : $title - $description - $image")

            if (title != null && title != episode.title) {
                episode.title = title
                needUpdate = true
            }

            if (description != null && description != episode.description) {
                episode.description = description
                needUpdate = true
            }

            if (image != episode.image) {
                episode.image = image
                ImageService.remove(episode.uuid!!, ImageService.Type.IMAGE)
                needUpdate = true
            }

            if (needUpdate) {
                episode.lastUpdateDateTime = now
                episodeService.update(episode)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while fetching episode description for $s", e)
        }

        return needUpdate
    }

    private fun normalizeUrl(platform: Platform, countryCode: CountryCode, url: String): String {
        return when (platform) {
            Platform.CRUN -> {
                val other = "https://www.crunchyroll.com/${countryCode.name.lowercase()}/"
                url.replace("https://www.crunchyroll.com/", other)
            }

            else -> url
        }
    }

    private suspend fun normalizeContent(
        episode: Episode,
        httpRequest: HttpRequest,
        accessToken: String,
        cms: CrunchyrollWrapper.CMS
    ): JsonObject? {
        return when (episode.platform) {
            Platform.CRUN -> {
                try {
                    httpRequest.getBrowser(
                        normalizeUrl(
                            episode.platform!!,
                            episode.anime!!.countryCode!!,
                            episode.url!!
                        )
                    )
                } catch (e: Exception) {
                    return null
                }

                val id = normalizeUrl(httpRequest.lastPageUrl!!)
                CrunchyrollWrapper.getObject(episode.anime!!.countryCode!!.locale, accessToken, cms, id)[0]
            }

            else -> {
                val split = episode.url!!.split("/")
                val videoId = split[split.size - 1].split("-")[0].toInt()
                AnimationDigitalNetworkWrapper.getShowVideo(videoId)
            }
        }
    }

    fun normalizeUrl(url: String) = "/watch/([A-Z0-9]+)".toRegex().find(url)!!.groupValues[1]

    private fun normalizeTitle(platform: Platform, content: JsonObject): String? {
        var title = when (platform) {
            Platform.CRUN -> content.getAsString("title")
            else -> content.getAsString("name")
        }

        title = title?.replace("\n", "")
        title = title?.replace("\r", "")
        title = title?.trim()
        return title
    }

    fun normalizeDescription(platform: Platform, content: JsonObject): String? {
        var description = when (platform) {
            Platform.CRUN -> content.getAsString("description")
            else -> content.getAsString("summary")
        }

        description = description?.replace("\n", "")
        description = description?.replace("\r", "")
        description = description?.trim()
        return description
    }

    fun normalizeImage(platform: Platform, content: JsonObject): String {
        return when (platform) {
            Platform.CRUN -> {
                val thumbnailArray =
                    requireNotNull(content.getAsJsonObject("images").getAsJsonArray("thumbnail")) { IMAGE_NULL_ERROR }
                val biggestImage =
                    requireNotNull(thumbnailArray[0].asJsonArray.maxByOrNull { it.asJsonObject.getAsInt("width")!! }) { IMAGE_NULL_ERROR }
                requireNotNull(
                    biggestImage.asJsonObject.getAsString("source")?.takeIf { it.isNotBlank() }) { IMAGE_NULL_ERROR }
            }

            else -> content.getAsString("image2x")!!
        }
    }
}