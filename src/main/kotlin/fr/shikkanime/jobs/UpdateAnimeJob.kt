package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.*
import fr.shikkanime.wrappers.impl.caches.*
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    data class UpdatableAnime(
        val platform: Platform,
        val name: String,
        val englishName: String? = null,
        val lastReleaseDateTime: ZonedDateTime,
        val attachments: Map<ImageType, String>,
        val description: String?,
        val episodeSize: Int
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService

    override fun run() {
        val currentDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val updateDelayDays = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_DELAY, 30).toLong()
        val updateBatchSize = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_SIZE, 15)
        
        val animes = animeService.findAllNeedUpdate(currentDateTime.minusDays(updateDelayDays))
        logger.info("Found ${animes.size} animes to update")

        val animesToUpdate = animes.shuffled().take(updateBatchSize)

        if (animesToUpdate.isEmpty()) {
            logger.info("No anime to update")
            return
        }

        animesToUpdate.forEach { anime ->
            updateSingleAnime(anime, currentDateTime)
        }

        MapCache.invalidate(Anime::class.java)
    }

    private fun updateSingleAnime(anime: Anime, currentDateTime: ZonedDateTime) {
        val shortName = StringUtils.getShortName(anime.name!!)
        logger.info("Updating anime $shortName...")
        
        val updatedAnimes = fetchUpdatedAnimeData(anime)

        if (updatedAnimes.isEmpty()) {
            logger.warning("No platform found for anime $shortName")
            anime.lastUpdateDateTime = currentDateTime
            animeService.update(anime)
            return
        }

        var hasChanged = false
        
        val updateAttachments = updatedAnimes.flatMap { it.attachments.entries }
            .groupBy { it.key }
            .mapValues { it.value.first().value }

        updateAttachments.forEach { (type, url) ->
            if (updateAttachment(anime, type, url)) {
                logger.info("Attachment $type updated for anime $shortName to $url")
            }
        }

        val updatableDescription = updatedAnimes.firstOrNull { !it.description.isNullOrBlank() }?.description?.normalize()

        if (updateDescription(anime, updatableDescription)) {
            logger.info("Description updated for anime $shortName to $updatableDescription")
            hasChanged = true
        }

        anime.lastUpdateDateTime = currentDateTime
        animeService.update(anime)
        
        if (hasChanged) {
            traceActionService.createTraceAction(anime, TraceAction.Action.UPDATE)
        }

        logger.info("Anime $shortName updated")
        updateExternalPlatformIds(updatedAnimes, shortName)
    }

    private fun updateExternalPlatformIds(updatedAnimes: List<UpdatableAnime>, shortName: String) {
        val names = updatedAnimes.map { it.englishName ?: it.name }.distinctBy { it.lowercase().filter { it.isLetterOrDigit() } }
        logger.info("Search for anime $shortName on AniList...")
        val media = names.asSequence()
            .flatMap { runBlocking { AniListCachedWrapper.search(it).toList() } }
            .firstOrNull()

        if (media == null) {
            logger.warning("No anime found on AniList for $shortName")
            return
        }

        logger.info("Anime found on AniList: $media")
    }

    private fun updateAttachment(anime: Anime, type: ImageType, url: String): Boolean {
        val existingAttachment = attachmentService.findByEntityUuidTypeAndActive(anime.uuid!!, type)
        if (existingAttachment?.url != url && url.isNotBlank()) {
            attachmentService.createAttachmentOrMarkAsActive(anime.uuid, type, url = url)
            return true
        }
        return false
    }
    
    private fun updateDescription(anime: Anime, newDescription: String?): Boolean {
        if (newDescription != anime.description && !newDescription.isNullOrBlank()) {
            anime.description = newDescription
            return true
        }
        return false
    }

    private fun fetchUpdatedAnimeData(anime: Anime): List<UpdatableAnime> {
        return runCatching { 
            runBlocking { fetchAnime(anime) } 
        }
        .getOrNull()
        ?.let { sortUpdatedAnimes(it) }
        ?: emptyList()
    }
    
    private fun sortUpdatedAnimes(animes: List<UpdatableAnime>): List<UpdatableAnime> {
        return animes.groupBy { it.platform }
            .toList()
            .sortedWith(compareBy({ it.second.size }, { it.first.sortIndex }))
            .flatMap { 
                it.second.sortedWith(
                    compareByDescending<UpdatableAnime> { it.lastReleaseDateTime }
                    .thenBy { it.episodeSize }
                ) 
            }
    }

    private suspend fun getAllAnimePlatformIds(anime: Anime): List<AnimePlatform> {
        logger.info("Fetching all anime platform IDs for ${anime.name}...")

        val platformIds = mutableMapOf<Platform, MutableSet<String>>()
        val episodeIds = mutableMapOf<Platform, MutableSet<String>>()

        episodeVariantService.findAllByAnimeUUID(anime.uuid!!, setOf(anime.countryCode!!.locale)).forEach { variant ->
            val platform = variant.platform!!

            val episodeId = when (platform) {
                Platform.ANIM -> AbstractAnimationDigitalNetworkWrapper.getAnimationDigitalNetworkId(variant.identifier!!)
                Platform.CRUN -> AbstractCrunchyrollWrapper.getCrunchyrollId(variant.identifier!!)
                Platform.DISN -> AbstractDisneyPlusWrapper.getDisneyPlusId(variant.identifier!!)
                Platform.NETF -> AbstractNetflixWrapper.getShowId(variant.url!!)
                Platform.PRIM -> AbstractPrimeVideoWrapper.getShowId(variant.url!!)
            }

            if (episodeId.isNullOrBlank()) {
                logger.warning("No episode ID found for anime ${anime.name} on platform $platform")
                return@forEach
            }

            episodeIds.computeIfAbsent(platform) { mutableSetOf() }.add(episodeId)
        }

        episodeIds.forEach { (platform, ids) ->
            val platformIdSet = platformIds.computeIfAbsent(platform) { mutableSetOf() }

            when (platform) {
                Platform.ANIM -> platformIdSet.addAll(ids.map { AnimationDigitalNetworkCachedWrapper.getVideo(it.toInt()).show.id.toString() })
                Platform.CRUN -> platformIdSet.addAll(CrunchyrollCachedWrapper.getObjects(anime.countryCode.locale, *ids.toTypedArray()).map { it.episodeMetadata!!.seriesId })
                Platform.DISN -> platformIdSet.addAll(ids.map { DisneyPlusCachedWrapper.getShowIdByEpisodeId(it).showId })
                Platform.NETF, Platform.PRIM -> platformIdSet.addAll(ids)
            }
        }

        logger.info("Fetched platform IDs for anime ${anime.name}: $platformIds")
        return platformIds.flatMap { (platform, ids) -> ids.map { platformId -> AnimePlatform(anime = anime, platform = platform, platformId = platformId) } }
    }

    private suspend fun fetchAnime(anime: Anime): List<UpdatableAnime> {
        // Fetch all anime platform IDs
        val animePlatforms = getAllAnimePlatformIds(anime)

        if (animePlatforms.isNotEmpty()) {
            // Delete existing platforms and add new ones
            animePlatformService.deleteByAnime(anime)

            animePlatforms.forEach { animePlatform ->
                animePlatformService.save(animePlatform)
            }
        }

        return (animePlatforms.takeIf { it.isNotEmpty() } ?: animePlatformService.findAllByAnime(anime)).mapNotNull { platform ->
            runCatching {
                fetchAnimeFromPlatform(platform)
            }.getOrElse { e ->
                logger.warning("Error fetching anime ${anime.name} from ${platform.platform}: ${e.message}")
                null
            }
        }
    }
    
    private suspend fun fetchAnimeFromPlatform(platform: AnimePlatform): UpdatableAnime {
        return when (platform.platform!!) {
            Platform.ANIM -> fetchADNAnime(platform)
            Platform.CRUN -> fetchCrunchyrollAnime(platform)
            Platform.DISN -> fetchDisneyPlusAnime(platform)
            Platform.NETF -> fetchNetflixAnime(platform)
            Platform.PRIM -> fetchPrimeVideoAnime(platform)
        }
    }

    private suspend fun fetchADNAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val platformId = animePlatform.platformId!!.toInt()
        val show = AnimationDigitalNetworkCachedWrapper.getShow(platformId)
        val showVideos = AnimationDigitalNetworkCachedWrapper.getShowVideos(platformId)

        if (showVideos.isEmpty()) {
            throw Exception("No episode found for ADN anime ${show.title}")
        }

        return UpdatableAnime(
            platform = Platform.ANIM,
            name = (show.originalTitle?.takeIf { !it.contains("??") && "[ぁ-んァ-ン-ー]".toRegex().containsMatchIn(it) } ?: show.title).replace("(VOSTFR)", "").trim(),
            lastReleaseDateTime = showVideos.maxOf { it.releaseDate },
            attachments = buildMap {
                put(ImageType.THUMBNAIL, show.fullHDImage)
                put(ImageType.BANNER, show.fullHDBanner)
            },
            description = show.summary,
            episodeSize = showVideos.size
        )
    }

    private suspend fun fetchCrunchyrollAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val anime = animePlatform.anime!!
        val countryCode = anime.countryCode!!
        val platformId = animePlatform.platformId!!
        
        val series = CrunchyrollCachedWrapper.getSeries(countryCode.locale, platformId)
        val episodes = CrunchyrollCachedWrapper.getEpisodesBySeriesId(
            countryCode.locale,
            series.id,
            true
        ).mapNotNull {
            runCatching {
                crunchyrollPlatform.convertEpisode(countryCode, it, false)
            }.getOrNull()
        }

        if (episodes.isEmpty()) {
            throw Exception("No episode found for Crunchyroll anime ${series.title}")
        }

        return UpdatableAnime(
            platform = Platform.CRUN,
            name = series.title,
            englishName = CrunchyrollCachedWrapper.getSeries("en-US", platformId).title,
            lastReleaseDateTime = episodes.maxOf { it.releaseDateTime },
            attachments = buildMap {
                put(ImageType.THUMBNAIL, series.fullHDImage!!)
                put(ImageType.BANNER, series.fullHDBanner!!)
            },
            description = series.description,
            episodeSize = episodes.size
        )
    }

    private suspend fun fetchDisneyPlusAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val anime = animePlatform.anime!!
        val platformId = animePlatform.platformId!!
        val countryCode = anime.countryCode!!
        val checkAudioLocales = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
        
        val show = DisneyPlusCachedWrapper.getShow(platformId)
        val episodes = DisneyPlusCachedWrapper.getEpisodesByShowId(
            countryCode.locale, 
            platformId, 
            checkAudioLocales
        )

        if (episodes.isEmpty()) {
            throw Exception("No episode found for Disney+ anime ${anime.name}")
        }

        return UpdatableAnime(
            platform = Platform.DISN,
            name = show.name,
            lastReleaseDateTime = anime.lastReleaseDateTime,
            attachments = buildMap {
                put(ImageType.THUMBNAIL, show.image)
                put(ImageType.BANNER, show.banner)
            },
            description = show.description,
            episodeSize = episodes.size
        )
    }

    private fun fetchNetflixAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val anime = animePlatform.anime!!
        val episodes = runCatching {
            NetflixCachedWrapper.getShowVideos(
                anime.countryCode!!,
                animePlatform.platformId!!
            )
        }.getOrNull()

        if (episodes.isNullOrEmpty()) {
            throw Exception("No episode found for Netflix anime ${anime.name}")
        }

        val show = episodes.first().show
        return UpdatableAnime(
            platform = Platform.NETF,
            name = show.name,
            lastReleaseDateTime = anime.lastReleaseDateTime,
            attachments = buildMap { put(ImageType.BANNER, show.banner) },
            description = show.description,
            episodeSize = episodes.size
        )
    }

    private fun fetchPrimeVideoAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val anime = animePlatform.anime!!
        val episodes = runCatching {
            PrimeVideoCachedWrapper.getShowVideos(
                anime.countryCode!!,
                animePlatform.platformId!!
            )
        }.getOrNull()

        if (episodes.isNullOrEmpty()) {
            throw Exception("No episode found for Prime Video anime ${anime.name}")
        }

        val show = episodes.first().show
        return UpdatableAnime(
            platform = Platform.PRIM,
            name = show.name,
            lastReleaseDateTime = anime.lastReleaseDateTime,
            attachments = buildMap { put(ImageType.BANNER, show.banner) },
            description = show.description,
            episodeSize = episodes.size
        )
    }
}