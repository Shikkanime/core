package fr.shikkanime.jobs

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

class FetchDeprecatedEpisodeJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var episodeService: EpisodeService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun run() {
        val httpRequest = HttpRequest()
        val anonymousAccessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(anonymousAccessToken) }

        val takeSize = configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODE_DESCRIPTION_SIZE, 0)

        val now = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val deprecatedDateTime = now.minusDays(
            configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_DEPRECATED_EPISODE_DATE, 30).toLong()
        )

        val crunchyrollEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.CRUN, deprecatedDateTime)
        val adnEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.ANIM, deprecatedDateTime)

        val episodes = (crunchyrollEpisodes + adnEpisodes)
            .shuffled()
            .take(takeSize)

        logger.info("Found ${episodes.size} episodes")

        episodes.forEachIndexed { index, episode ->
            logger.info("Fetching episode description ${index + 1}/${episodes.size}")

            val s = "${episode.anime?.name} - S${episode.season} ${
                when (episode.episodeType!!) {
                    EpisodeType.EPISODE -> "EP"
                    EpisodeType.SPECIAL -> "SP"
                    EpisodeType.FILM -> "MOV"
                }
            }${episode.number}"

            try {
                val content = runBlocking { normalizeContent(episode, httpRequest, anonymousAccessToken, cms) } ?: return@forEachIndexed
                val title = normalizeTitle(episode.platform!!, content)
                val description = normalizeDescription(episode.platform!!, content)
                logger.config("$s : $title - $description")
                episode.title = title
                episode.description = description
                episode.lastUpdateDateTime = now
                episodeService.update(episode)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while fetching episode description for $s", e)
            }
        }

        if (episodes.isNotEmpty()) {
            MapCache.invalidate(Episode::class.java)
        }

        httpRequest.close()
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

    private suspend fun normalizeContent(episode: Episode, httpRequest: HttpRequest, accessToken: String, cms: CrunchyrollWrapper.CMS): JsonObject? {
        return when (episode.platform) {
            Platform.CRUN -> {
                try {
                    httpRequest.getBrowser(normalizeUrl(episode.platform!!, episode.anime!!.countryCode!!, episode.url!!))
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
}