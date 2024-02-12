package fr.shikkanime.jobs

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.util.logging.Level

class FetchOldEpisodeDescriptionJob : AbstractJob() {
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

        val crunchyrollEpisodes = episodeService.findAllByPlatform(Platform.CRUN)
        val adnEpisodes = episodeService.findAllByPlatform(Platform.ANIM)

        val episodes = (crunchyrollEpisodes + adnEpisodes)
            .filter { it.description.isNullOrBlank() }
            .shuffled()
            .take(takeSize)

        logger.info("Found ${episodes.size} episodes")

        episodes.forEachIndexed { index, episode ->
            logger.info("Fetching episode description ${index + 1}/${episodes.size}")
            val s = "${episode.anime?.name} - S${episode.season} EP${episode.number}"

            try {
                val content = runBlocking { normalizeContent(episode, httpRequest, anonymousAccessToken, cms) } ?: return@forEachIndexed
                val description = normalizeDescription(episode, content)
                logger.config("$s : $description")
                episode.description = description
                episodeService.update(episode)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while fetching episode description for $s", e)
            }
        }

        httpRequest.close()
    }

    private fun normalizeUrl(episode: Episode): String {
        return when (episode.platform) {
            Platform.CRUN -> {
                val other = "https://www.crunchyroll.com/${episode.anime?.countryCode?.name?.lowercase()}/"
                episode.url!!.replace("https://www.crunchyroll.com/", other)
            }

            else -> episode.url!!
        }
    }

    private suspend fun normalizeContent(episode: Episode, httpRequest: HttpRequest, accessToken: String, cms: CrunchyrollWrapper.CMS): JsonObject? {
        return when (episode.platform) {
            Platform.CRUN -> {
                try {
                    httpRequest.getBrowser(normalizeUrl(episode))
                } catch (e: Exception) {
                    return null
                }

                val id = normalizeUrl(httpRequest.lastPageUrl!!)
                CrunchyrollWrapper.getObject(episode.anime!!.countryCode!!.locale, accessToken, cms, id)[0]
            }

            else -> {
                val split = episode.url!!.split("/")
                val animeNameEncoded = split[split.size - 2]
                val animeId = AnimationDigitalNetworkWrapper.getShow(animeNameEncoded).getAsInt("id")!!
                val videoId = split[split.size - 1].split("-")[0].toInt()
                AnimationDigitalNetworkWrapper.getShowVideos(animeId, videoId)[0]
            }
        }
    }

    fun normalizeUrl(url: String) ="/watch/([A-Z0-9]+)/".toRegex().find(url)!!.groupValues[1]

    private fun normalizeDescription(episode: Episode, content: JsonObject): String? {
        var description = when (episode.platform) {
            Platform.CRUN -> content.getAsString("description")
            else -> content.getAsString("summary")
        }

        description = description?.replace("\n", "")
        description = description?.replace("\r", "")
        description = description?.trim()
        return description
    }
}