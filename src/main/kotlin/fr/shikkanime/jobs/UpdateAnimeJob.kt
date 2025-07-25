package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.impl.caches.*
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    data class UpdatableAnime(
        val platform: Platform,
        val lastReleaseDateTime: ZonedDateTime,
        val attachments: Map<ImageType, String>,
        val description: String?,
        val episodeSize: Int,
        var isValidated: Boolean? = null
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var attachmentService: AttachmentService

    override suspend fun run() {
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val animes = animeService.findAllNeedUpdate()
        logger.info("Found ${animes.size} animes to update")

        val needUpdateAnimes = animes.shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_SIZE, 15))

        if (needUpdateAnimes.isEmpty()) {
            logger.info("No anime to update")
            return
        }

        val deprecatedAnimePlatformDateTime = zonedDateTime.minusMonths(configCacheService.getValueAsInt(ConfigPropertyKey.ANIME_PLATFORM_DEPRECATED_DURATION, 3).toLong())

        needUpdateAnimes.forEach { anime ->
            val shortName = StringUtils.getShortName(anime.name!!)
            logger.info("Updating anime $shortName...")
            // Compare platform sort index and anime release date descending
            val updatedAnimes = runCatching { fetchAnime(anime, deprecatedAnimePlatformDateTime) }
                .getOrNull()
                ?.groupBy { it.platform }
                ?.toList()
                ?.sortedWith(
                    compareBy<Pair<Platform, List<UpdatableAnime>>> { it.second.size }
                        .thenBy { it.first.sortIndex }
                )
                ?.flatMap { pair ->
                    pair.second.sortedWith(
                        compareByDescending<UpdatableAnime> { it.isValidated }
                            .thenByDescending { it.lastReleaseDateTime }
                            .thenBy { it.episodeSize }
                    )
                } ?: emptyList()

            if (updatedAnimes.isEmpty()) {
                logger.warning("No platform found for anime $shortName")
                anime.lastUpdateDateTime = zonedDateTime
                animeService.update(anime)
                return@forEach
            }

            var hasChanged = false

            val updateAttachments = updatedAnimes.flatMap { it.attachments.entries }
                .groupBy { it.key }
                .mapValues { it.value.first().value }

            updateAttachments.forEach { (type, url) ->
                if (attachmentService.findByEntityUuidTypeAndActive(anime.uuid!!, type)?.url != url && url.isNotBlank()) {
                    attachmentService.createAttachmentOrMarkAsActive(anime.uuid, type, url = url)
                    logger.info("Attachment $type updated for anime $shortName to $url")
                }
            }

            val updatableDescription = updatedAnimes.firstOrNull { !it.description.isNullOrBlank() }?.description?.normalize()

            if (updatableDescription != anime.description && !updatableDescription.isNullOrBlank()) {
                anime.description = updatableDescription
                logger.info("Description updated for anime $shortName to $updatableDescription")
                hasChanged = true
            }

            anime.lastUpdateDateTime = zonedDateTime
            animeService.update(anime)

            if (hasChanged) {
                traceActionService.createTraceAction(anime, TraceAction.Action.UPDATE)
            }

            logger.info("Anime $shortName updated")
        }

        InvalidationService.invalidate(Anime::class.java)
    }

    private suspend fun fetchAnime(anime: Anime, zonedDateTime: ZonedDateTime): List<UpdatableAnime> {
        val list = mutableListOf<UpdatableAnime>()

        animePlatformService.findAllByAnime(anime)
            .filter { it.platform!!.isStreamingPlatform }
            .forEach {
                if (it.lastValidateDateTime != null && it.lastValidateDateTime!!.isBeforeOrEqual(zonedDateTime)) {
                    logger.warning("Deleting old anime platform ${it.platform} for anime ${anime.name} with id ${it.platformId}")
                    animePlatformService.delete(it)
                    traceActionService.createTraceAction(it, TraceAction.Action.DELETE)
                    return@forEach
                }

                runCatching {
                    val updatableAnime = when (it.platform!!) {
                        Platform.ANIM -> fetchADNAnime(it)
                        Platform.CRUN -> fetchCrunchyrollAnime(it)
                        Platform.DISN -> fetchDisneyPlusAnime(it)
                        Platform.NETF -> fetchNetflixAnime(it)
                        Platform.PRIM -> fetchPrimeVideoAnime(it)
                        else -> throw Exception("Invalid platform ${it.platform}")
                    }

                    updatableAnime.isValidated = it.lastValidateDateTime != null && it.lastValidateDateTime!!.isAfterOrEqual(zonedDateTime)
                    list.add(updatableAnime)
                }.onFailure { e ->
                    logger.warning("Error while fetching anime ${anime.name} on platform ${it.platform}: ${e.message}")
                }
            }

        return list
    }

    private suspend fun fetchADNAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val countryCode = animePlatform.anime!!.countryCode!!
        val platformId = animePlatform.platformId!!.toInt()
        val show = AnimationDigitalNetworkCachedWrapper.getShow(countryCode.name, platformId)
        val showVideos = AnimationDigitalNetworkCachedWrapper.getShowVideos(countryCode, platformId)
            .filter { it.releaseDate != null }

        require(showVideos.isNotEmpty()) { "No episode found for ADN anime ${show.title}" }

        return UpdatableAnime(
            platform = Platform.ANIM,
            lastReleaseDateTime = showVideos.maxOf { it.releaseDate!! },
            attachments = mapOf(
                ImageType.THUMBNAIL to show.fullHDImage,
                ImageType.BANNER to show.fullHDBanner,
                ImageType.CAROUSEL to show.fullHDCarousel
            ),
            description = show.summary,
            episodeSize = showVideos.size
        )
    }

    private suspend fun fetchCrunchyrollAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val countryCode = animePlatform.anime!!.countryCode!!
        val series = CrunchyrollCachedWrapper.getObjects(countryCode.locale, animePlatform.platformId!!).first()

        val objects = CrunchyrollCachedWrapper.getEpisodesBySeriesId(
            countryCode.locale,
            series.id,
            true
        ).mapNotNull {
            runCatching {
                crunchyrollPlatform.convertEpisode(
                    countryCode,
                    it,
                    false
                )
            }.getOrNull()
        }

        if (objects.isEmpty())
            throw Exception("No episode found for Crunchyroll anime ${series.title}")

        return UpdatableAnime(
            platform = Platform.CRUN,
            lastReleaseDateTime = objects.maxOf { it.releaseDateTime },
            attachments = mapOf(
                ImageType.THUMBNAIL to series.images!!.fullHDImage!!,
                ImageType.BANNER to series.images.fullHDBanner!!,
                ImageType.CAROUSEL to series.fullHDCarousel
            ),
            description = series.getNormalizedDescription(),
            episodeSize = objects.size
        )
    }

    private suspend fun fetchDisneyPlusAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val show = DisneyPlusCachedWrapper.getShow(animePlatform.platformId!!)
        val episodes = DisneyPlusCachedWrapper.getEpisodesByShowId(animePlatform.anime!!.countryCode!!, animePlatform.platformId!!, configCacheService.getValueAsBoolean(
            ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES))

        if (episodes.isEmpty())
            throw Exception("No episode found for Disney+ anime ${animePlatform.anime!!.name}")

        return UpdatableAnime(
            platform = Platform.DISN,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            attachments = mapOf(
                ImageType.THUMBNAIL to show.image,
                ImageType.BANNER to show.banner,
                ImageType.CAROUSEL to show.carousel
            ),
            description = show.description,
            episodeSize = episodes.size
        )
    }

    private suspend fun fetchNetflixAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val episodes = runCatching {
            NetflixCachedWrapper.getEpisodesByShowId(
                animePlatform.anime!!.countryCode!!,
                animePlatform.platformId!!.toInt()
            )
        }.getOrNull()

        if (episodes.isNullOrEmpty())
            throw Exception("No episode found for Netflix anime ${animePlatform.anime!!.name}")

        val show = episodes.first().show

        return UpdatableAnime(
            platform = Platform.NETF,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            attachments = buildMap {
                show.thumbnail?.let { put(ImageType.THUMBNAIL, it) }
                put(ImageType.BANNER, show.banner)
                put(ImageType.CAROUSEL, show.carousel)
            },
            description = show.description,
            episodeSize = episodes.size
        )
    }

    private suspend fun fetchPrimeVideoAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val episodes = runCatching {
            HttpRequest.retry(3) {
                PrimeVideoCachedWrapper.getEpisodesByShowId(
                    animePlatform.anime!!.countryCode!!,
                    animePlatform.platformId!!
                )
            }
        }.getOrNull()

        if (episodes.isNullOrEmpty())
            throw Exception("No episode found for Prime Video anime ${animePlatform.anime!!.name}")

        val show = episodes.first().show

        return UpdatableAnime(
            platform = Platform.PRIM,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            attachments = mapOf(
                ImageType.BANNER to show.banner,
                ImageType.CAROUSEL to show.carousel
            ),
            description = show.description,
            episodeSize = episodes.size
        )
    }
}