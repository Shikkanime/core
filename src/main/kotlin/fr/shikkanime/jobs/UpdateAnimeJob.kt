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
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    data class UpdatableAnime(
        val platform: Platform,
        val lastReleaseDateTime: ZonedDateTime,
        val attachments: Map<ImageType, String>,
        val description: String?,
        val episodeSize: Int
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    @Inject
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var attachmentService: AttachmentService

    override fun run() {
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val lastDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_DELAY, 30).toLong())
        val animes = animeService.findAllNeedUpdate(lastDateTime)
        logger.info("Found ${animes.size} animes to update")

        val needUpdateAnimes = animes.shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_SIZE, 15))

        if (needUpdateAnimes.isEmpty()) {
            logger.info("No anime to update")
            return
        }

        needUpdateAnimes.forEach { anime ->
            val shortName = StringUtils.getShortName(anime.name!!)
            logger.info("Updating anime $shortName...")
            // Compare platform sort index and anime release date descending
            val updatedAnimes = runCatching { runBlocking { fetchAnime(anime) } }
                .getOrNull()
                ?.groupBy { it.platform }
                ?.toList()
                ?.sortedWith(
                    compareBy<Pair<Platform, List<UpdatableAnime>>> { it.second.size }
                        .thenBy { it.first.sortIndex }
                )
                ?.flatMap { pair ->
                    pair.second.sortedWith(
                        compareByDescending<UpdatableAnime> { it.lastReleaseDateTime }
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

        MapCache.invalidate(Anime::class.java)
    }

    private suspend fun fetchAnime(anime: Anime): List<UpdatableAnime> {
        val list = mutableListOf<UpdatableAnime>()

        animePlatformService.findAllByAnime(anime).forEach {
            runCatching {
                when (it.platform!!) {
                    Platform.ANIM -> list.add(fetchADNAnime(it))
                    Platform.CRUN -> list.add(fetchCrunchyrollAnime(it))
                    Platform.DISN -> list.add(fetchDisneyPlusAnime(it))
                    Platform.NETF -> list.add(fetchNetflixAnime(it))
                    Platform.PRIM -> list.add(fetchPrimeVideoAnime(it))
                }
            }.onFailure { e ->
                logger.warning("Error while fetching anime ${anime.name} on platform ${it.platform}: ${e.message}")
            }
        }

        return list
    }

    private suspend fun fetchADNAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val platformId = animePlatform.platformId!!.toInt()
        val show = AnimationDigitalNetworkCachedWrapper.getShow(platformId)
        val showVideos = AnimationDigitalNetworkCachedWrapper.getShowVideos(platformId)
            .filter { it.releaseDate != null }

        require(showVideos.isNotEmpty()) { "No episode found for ADN anime ${show.title}" }

        return UpdatableAnime(
            platform = Platform.ANIM,
            lastReleaseDateTime = showVideos.maxOf { it.releaseDate!! },
            attachments = mapOf(
                ImageType.THUMBNAIL to show.fullHDImage,
                ImageType.BANNER to show.fullHDBanner
            ),
            description = show.summary,
            episodeSize = showVideos.size
        )
    }

    private suspend fun fetchCrunchyrollAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val countryCode = animePlatform.anime!!.countryCode!!
        val series = CrunchyrollCachedWrapper.getSeries(countryCode.locale, animePlatform.platformId!!)

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
            attachments = buildMap {
                put(ImageType.THUMBNAIL, series.fullHDImage!!)
                put(ImageType.BANNER, series.fullHDBanner!!)
            },
            description = series.description,
            episodeSize = objects.size
        )
    }

    private suspend fun fetchDisneyPlusAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val show = DisneyPlusCachedWrapper.getShow(animePlatform.platformId!!)
        val episodes = DisneyPlusCachedWrapper.getEpisodesByShowId(animePlatform.anime!!.countryCode!!.locale, animePlatform.platformId!!, configCacheService.getValueAsBoolean(
            ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES))

        if (episodes.isEmpty())
            throw Exception("No episode found for Disney+ anime ${animePlatform.anime!!.name}")

        return UpdatableAnime(
            platform = Platform.DISN,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            attachments = buildMap {
                put(ImageType.THUMBNAIL, show.image)
                put(ImageType.BANNER, show.banner)
            },
            description = show.description,
            episodeSize = episodes.size
        )
    }

    private suspend fun fetchNetflixAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val episodes = runCatching {
            NetflixCachedWrapper.getEpisodesByShowId(
                animePlatform.anime!!.countryCode!!.locale,
                animePlatform.platformId!!.toInt(),
            )
        }.getOrNull()

        if (episodes.isNullOrEmpty())
            throw Exception("No episode found for Netflix anime ${animePlatform.anime!!.name}")

        val show = episodes.first().show

        return UpdatableAnime(
            platform = Platform.NETF,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            attachments = buildMap { put(ImageType.BANNER, show.banner) },
            description = show.description,
            episodeSize = episodes.size
        )
    }

    private fun fetchPrimeVideoAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val episodes = runCatching {
            PrimeVideoCachedWrapper.getShowVideos(
                animePlatform.anime!!.countryCode!!,
                animePlatform.platformId!!
            )
        }.getOrNull()

        if (episodes.isNullOrEmpty())
            throw Exception("No episode found for Prime Video anime ${animePlatform.anime!!.name}")

        val show = episodes.first().show

        return UpdatableAnime(
            platform = Platform.PRIM,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            attachments = buildMap { put(ImageType.BANNER, show.banner) },
            description = show.description,
            episodeSize = episodes.size
        )
    }
}