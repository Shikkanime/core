package fr.shikkanime.jobs

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeAnimeIdKeyCache
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.ConfigPropertyKey
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
import fr.shikkanime.wrappers.PrimeVideoWrapper
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.ZonedDateTime
import java.util.logging.Level

private const val IMAGE_NULL_ERROR = "Image is null"

class FetchDeprecatedEpisodeJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    var accessToken: String = ""
    lateinit var cms: CrunchyrollWrapper.CMS

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
        val crunchyrollEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(
            Platform.CRUN,
            deprecatedDateTime,
            "https://www.crunchyroll.com/fr/watch/%"
        )
        val primeVideoEpisodes = episodeService.findAllByPlatformDeprecatedEpisodes(Platform.PRIM, deprecatedDateTime)

        val episodes = (adnEpisodes + crunchyrollEpisodes + primeVideoEpisodes).shuffled().take(takeSize)

        logger.info("Found ${episodes.size} episodes")

        if (episodes.isEmpty()) {
            return
        }

        val httpRequest = HttpRequest()
        val anonymousAccessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        accessToken = anonymousAccessToken
        val cms = runBlocking { CrunchyrollWrapper.getCMS(anonymousAccessToken) }
        this.cms = cms
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

    private fun getIdentifier(episode: Episode) = "${episode.anime?.name} - S${episode.season} ${
        when (episode.episodeType!!) {
            EpisodeType.EPISODE -> "EP"
            EpisodeType.SPECIAL -> "SP"
            EpisodeType.FILM -> "MOV"
        }
    }${episode.number}"

    fun update(
        episode: Episode,
        httpRequest: HttpRequest,
        anonymousAccessToken: String,
        cms: CrunchyrollWrapper.CMS,
        now: ZonedDateTime,
    ): Boolean {
        var needUpdate = false
        val identifier = getIdentifier(episode)

        try {
            val content =
                runBlocking { normalizeContent(episode, httpRequest, anonymousAccessToken, cms) } ?: return false
            val title = normalizeTitle(episode.platform!!, content)
            val description = normalizeDescription(episode.platform!!, content)
            val image = normalizeImage(episode.platform!!, content)
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

            if (needUpdate) {
                episode.lastUpdateDateTime = now
                episodeService.update(episode)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while fetching episode description for $identifier", e)
        }

        return needUpdate
    }

    fun buildCrunchyrollEpisodeUrl(content: JsonObject, episode: Episode): String {
        val id = content.getAsString("id")!!
        val slugTitle = content.getAsString("slug_title")
        val url = CrunchyrollWrapper.buildUrl(episode.anime!!.countryCode!!, id, slugTitle)
        return url
    }

    private suspend fun normalizeContent(
        episode: Episode,
        httpRequest: HttpRequest,
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
                val id =
                    getCrunchyrollEpisodeId(episode.url!!) ?: return crunchyrollExternalIdToId(httpRequest, episode)
                CrunchyrollWrapper.getObject(episode.anime!!.countryCode!!.locale, accessToken, cms, id)[0]
            }

            Platform.PRIM -> {
                val id = episode.url!!.split("/").last()

                PrimeVideoWrapper.getShowVideos(
                    episode.anime!!.countryCode!!.name,
                    episode.anime!!.countryCode!!.locale,
                    id
                ).find { it.getAsString("id") == episode.hash }
            }

            else -> null
        }
    }

    private val episodesInfoCache = MapCache<CountryCodeAnimeIdKeyCache, List<JsonObject>>(Duration.ofDays(1)) {
        runBlocking {
            val episodes = CrunchyrollWrapper.getSeasons(it.countryCode.locale, accessToken, cms, it.animeId)
                .flatMap { season ->
                    CrunchyrollWrapper.getEpisodes(
                        it.countryCode.locale,
                        accessToken,
                        cms,
                        season.getAsString("id")!!
                    )
                }
                .map { it.getAsString("id")!! }
                .chunked(25)

            episodes.flatMap { chunk ->
                CrunchyrollWrapper.getObject(it.countryCode.locale, accessToken, cms, *chunk.toTypedArray())
            }
        }
    }

    fun crunchyrollExternalIdToId(
        httpRequest: HttpRequest,
        episode: Episode,
    ): JsonObject? {
        val selector = "div[data-t=\"search-series-card\"]"
        val titleSelector = "a[tabindex=\"0\"]"

        val content = try {
            httpRequest.getBrowser(
                "https://www.crunchyroll.com/${episode.anime!!.countryCode!!.name.lowercase()}/search?q=${
                    URLEncoder.encode(
                        episode.anime!!.name!!,
                        StandardCharsets.UTF_8
                    )
                }",
                selector
            )
        } catch (e: Exception) {
            return null
        }

        val seriesCard = content.select(selector)
        val serieCard = seriesCard.find { it.select(titleSelector).attr("title") == episode.anime!!.name }
            ?: throw Exception("Failed to find serie card for ${episode.anime!!.name}")
        val seriesId = getCrunchyrollSeriesId(serieCard.select(titleSelector).attr("href"))
            ?: throw Exception("Failed to find serie id for ${episode.anime!!.name}")
        val allEpisodes =
            episodesInfoCache[CountryCodeAnimeIdKeyCache(episode.anime!!.countryCode!!, seriesId)] ?: return null

        val externalId = "EPI.${getExternalId(episode.url!!)}"
        return allEpisodes.find { it.getAsString("external_id") == externalId }
    }

    private fun getExternalId(url: String) = "-([0-9]{6})".toRegex().find(url)?.groupValues?.get(1)

    private fun getCrunchyrollSeriesId(url: String) = "/series/([A-Z0-9]+)".toRegex().find(url)?.groupValues?.get(1)

    fun getCrunchyrollEpisodeId(url: String) = "/watch/([A-Z0-9]+)".toRegex().find(url)?.groupValues?.get(1)

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
}