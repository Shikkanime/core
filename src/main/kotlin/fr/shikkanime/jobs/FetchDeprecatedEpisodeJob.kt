package fr.shikkanime.jobs

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import fr.shikkanime.wrappers.PrimeVideoWrapper
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

        val adnEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.ANIM, deprecatedDateTime)
        val crunchyrollEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.CRUN, deprecatedDateTime)
        val primeVideoEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.PRIM, deprecatedDateTime)
        val episodes = (adnEpisodes + crunchyrollEpisodes + primeVideoEpisodes).shuffled().take(takeSize)

        logger.info("Found ${episodes.size} episodes")

        if (episodes.isEmpty()) {
            return
        }

        val anonymousAccessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(anonymousAccessToken) }
        var count = 0

        episodes.forEachIndexed { index, episode ->
            logger.info("Fetching episode description ${index + 1}/${episodes.size}")

            if (update(episode, anonymousAccessToken, cms, now)) {
                count++
            }
        }

        if (count <= 0) {
            return
        }

        logger.info("Updated $count episodes")
        MapCache.invalidate(Episode::class.java)
    }

    private fun getIdentifier(episode: Episode) = "${episode.anime?.name} - S${episode.season} ${
        when (episode.episodeType!!) {
            EpisodeType.EPISODE -> "EP"
            EpisodeType.SPECIAL -> "SP"
            EpisodeType.FILM -> "MOV"
        }
    }${episode.number}"

    fun update(
        episode: Episode,
        anonymousAccessToken: String,
        cms: CrunchyrollWrapper.CMS,
        now: ZonedDateTime,
    ): Boolean {
        var needUpdate = false
        val identifier = getIdentifier(episode)

        try {
            val content =
                runBlocking { normalizeContent(episode, anonymousAccessToken, cms) } ?: return false
            val title = normalizeTitle(episode.platform!!, content)
            val description = normalizeDescription(episode.platform!!, content)
            val image = normalizeImage(episode.platform!!, content)
            val audioLocale = normalizeAudioLocale(episode.platform!!, content)
            logger.config("$identifier : $title - $description - $image")

            if (title != null && title != episode.title) {
                episode.title = title
                needUpdate = true
            }

            if (description != null && description != episode.description) {
                episode.description = description
                needUpdate = true
            }

            if (episode.platform!! == Platform.CRUN) {
                val url = buildCrunchyrollEpisodeUrl(content, episode)

                if (url != episode.url) {
                    episode.url = url
                    needUpdate = true
                }
            }

            if (image != episode.image) {
                episode.image = image
                ImageService.remove(episode.uuid!!, ImageService.Type.IMAGE)
                episodeService.addImage(episode.uuid, image)
                needUpdate = true
            }

            if (audioLocale != episode.audioLocale) {
                episode.audioLocale = audioLocale
                needUpdate = true
            }

            if (episode.platform == Platform.CRUN) {
                val id = getCrunchyrollEpisodeId(episode.url!!) ?: return false
                val hash = StringUtils.getHash(episode.anime!!.countryCode!!, episode.platform!!, id, episode.langType!!)

                if (hash != episode.hash) {
                    episode.hash = hash
                    needUpdate = true
                }
            }

            episode.status = StringUtils.getStatus(episode)
            episode.lastUpdateDateTime = now
            episodeService.update(episode)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while fetching episode description for $identifier", e)
        }

        return needUpdate
    }

    private fun buildCrunchyrollEpisodeUrl(content: JsonObject, episode: Episode): String {
        val id = content.getAsString("id")!!
        val slugTitle = content.getAsString("slug_title")
        val url = CrunchyrollWrapper.buildUrl(episode.anime!!.countryCode!!, id, slugTitle)
        return url
    }

    private suspend fun normalizeContent(
        episode: Episode,
        accessToken: String,
        cms: CrunchyrollWrapper.CMS
    ): JsonObject? {
        return when (episode.platform) {
            Platform.ANIM -> {
                val split = episode.url!!.split("/")
                val videoId = split[split.size - 1].split("-")[0].toInt()
                AnimationDigitalNetworkWrapper.getShowVideo(videoId)
            }

            Platform.CRUN -> {
                val id = getCrunchyrollEpisodeId(episode.url!!) ?: return null
                CrunchyrollWrapper.getObject(episode.anime!!.countryCode!!.locale, accessToken, cms, id)[0]
            }

            Platform.PRIM -> {
                val id = episode.url!!.split("/").last()

                PrimeVideoWrapper.getShowVideos(
                    episode.anime!!.countryCode!!.name,
                    episode.anime!!.countryCode!!.locale,
                    id
                ).find { episode.hash!!.contains(it.getAsString("id")!!, true) }
            }

            else -> null
        }
    }

    fun getCrunchyrollEpisodeId(url: String) = "/watch/([A-Z0-9]{9})".toRegex().find(url)?.groupValues?.get(1)

    fun normalizeTitle(platform: Platform, content: JsonObject): String? {
        var title = when (platform) {
            Platform.CRUN, Platform.PRIM -> content.getAsString("title")
            else -> content.getAsString("name")
        }

        title = title?.replace("\n", "")
        title = title?.replace("\r", "")
        title = title?.trim()
        return title.takeIf { !it.isNullOrBlank() }
    }

    fun normalizeDescription(platform: Platform, content: JsonObject): String? {
        var description = when (platform) {
            Platform.CRUN, Platform.PRIM -> content.getAsString("description")
            else -> content.getAsString("summary")
        }

        description = description?.replace("\n", "")
        description = description?.replace("\r", "")
        description = description?.trim()
        return description.takeIf { !it.isNullOrBlank() }
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

            Platform.PRIM -> content.getAsString("image")!!
            else -> content.getAsString("image2x")!!
        }
    }

    private fun normalizeAudioLocale(platform: Platform, content: JsonObject): String {
        return when (platform) {
            Platform.CRUN -> requireNotNull(content.getAsJsonObject("episode_metadata").getAsString("audio_locale")) { "Audio locale is null" }
            else -> "ja-JP"
        }
    }
}