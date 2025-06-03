package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.impl.caches.*
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    data class UpdatableAnime(
        val platform: Platform,
        val attachments: Map<ImageType, String>,
        val description: String?,
        val identifiers: Set<String>
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService

    @Inject private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform

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

        val deprecatedAnimePlatformDateTime = zonedDateTime.minusMonths(configCacheService.getValueAsInt(ConfigPropertyKey.ANIME_PLATFORM_DEPRECATED_DURATION, 3).toLong())

        needUpdateAnimes.forEach { anime ->
            val shortName = StringUtils.getShortName(anime.name!!)
            logger.info("Updating anime $shortName...")
            // Compare platform sort index and anime release date descending
            val updatedAnimes = runCatching { runBlocking { fetchAnime(anime, deprecatedAnimePlatformDateTime) } }
                .getOrNull()
                ?.groupBy { it.platform }
                ?.toList()
                ?.sortedWith(
                    compareBy<Pair<Platform, List<UpdatableAnime>>> { it.second.size }
                        .thenBy { it.first.sortIndex }
                )
                ?.flatMap { pair ->
                    pair.second.sortedWith(
                        compareByDescending<UpdatableAnime> { episodeVariantService.findMinimalMappingDateTimeByIdentifiers(it.identifiers) }
                            .thenBy { it.identifiers.size }
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

    private suspend fun fetchAnime(anime: Anime, zonedDateTime: ZonedDateTime): List<UpdatableAnime> {
        val list = mutableListOf<UpdatableAnime>()

        animePlatformService.findAllByAnime(anime).forEach {
            if (it.lastValidateDateTime != null && it.lastValidateDateTime!!.isBeforeOrEqual(zonedDateTime)) {
                logger.warning("Deleting old anime platform ${it.platform} for anime ${anime.name} with id ${it.platformId}")
                animePlatformService.delete(it)
                return@forEach
            }

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

        val identifiers = AnimationDigitalNetworkCachedWrapper.getShowVideos(platformId)
            .flatMap { video ->
                video.languages.map { language ->
                    StringUtils.getIdentifier(
                        animePlatform.anime!!.countryCode!!,
                        animePlatform.platform!!,
                        video.id.toString(),
                        animationDigitalNetworkPlatform.getAudioLocale(language),
                        video.title.contains("(NC)", true) || video.title.contains("Non censuré", true)
                    )
                }
            }.toSet()

        require(identifiers.isNotEmpty()) { "No episode found for ADN anime ${show.title}" }

        return UpdatableAnime(
            platform = Platform.ANIM,
            attachments = mapOf(
                ImageType.THUMBNAIL to show.fullHDImage,
                ImageType.BANNER to show.fullHDBanner
            ),
            description = show.summary,
            identifiers = identifiers
        )
    }

    private suspend fun fetchCrunchyrollAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val countryCode = animePlatform.anime!!.countryCode!!
        val series = CrunchyrollCachedWrapper.getSeries(countryCode.locale, animePlatform.platformId!!)

        val identifiers = CrunchyrollCachedWrapper.getEpisodesBySeriesId(
            countryCode.locale,
            series.id,
            true
        ).map {
           StringUtils.getIdentifier(
                countryCode,
                animePlatform.platform!!,
                it.id,
                it.episodeMetadata!!.audioLocale,
                it.episodeMetadata.matureBlocked
            )
        }.toSet()

        require(identifiers.isNotEmpty()) { "No episode found for Crunchyroll anime ${series.title}" }

        return UpdatableAnime(
            platform = Platform.CRUN,
            attachments = buildMap {
                put(ImageType.THUMBNAIL, series.fullHDImage!!)
                put(ImageType.BANNER, series.fullHDBanner!!)
            },
            description = series.description,
            identifiers = identifiers,
        )
    }

    private suspend fun fetchDisneyPlusAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val countryCode = animePlatform.anime!!.countryCode!!
        val platformId = animePlatform.platformId!!

        val show = DisneyPlusCachedWrapper.getShow(platformId)
        val identifiers = DisneyPlusCachedWrapper.getEpisodesByShowId(countryCode.locale, platformId, configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES))
            .flatMap { episode ->
                episode.audioLocales.map { audioLocale ->
                    StringUtils.getIdentifier(
                        countryCode,
                        animePlatform.platform!!,
                        episode.show.id,
                        audioLocale
                    )
                }
            }.toSet()

        require(identifiers.isNotEmpty()) { "No episode found for Disney+ anime ${show.name}" }

        return UpdatableAnime(
            platform = Platform.DISN,
            attachments = buildMap {
                put(ImageType.THUMBNAIL, show.image)
                put(ImageType.BANNER, show.banner)
            },
            description = show.description,
            identifiers = identifiers,
        )
    }

    private suspend fun fetchNetflixAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val audioLocales = animeCacheService.getAudioLocales(animePlatform.anime!!)!!

        val locale = animePlatform.anime!!.countryCode!!.locale
        val platformId = animePlatform.platformId!!.toInt()
        val show = NetflixCachedWrapper.getShow(locale, platformId)

        val identifiers = NetflixCachedWrapper.getEpisodesByShowId(
            locale,
            platformId,
        ).flatMap { episode ->
            audioLocales.map { audioLocale ->
                StringUtils.getIdentifier(
                    animePlatform.anime!!.countryCode!!,
                    animePlatform.platform!!,
                    episode.id.toString(),
                    audioLocale
                )
            }
        }.toSet()

        require(identifiers.isNotEmpty()) { "No episode found for Netflix anime ${show.name}" }

        return UpdatableAnime(
            platform = Platform.NETF,
            attachments = buildMap { put(ImageType.BANNER, show.banner) },
            description = show.description,
            identifiers = identifiers,
        )
    }

    private suspend fun fetchPrimeVideoAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val audioLocales = animeCacheService.getAudioLocales(animePlatform.anime!!)!!
        val episodes = PrimeVideoCachedWrapper.getEpisodesByShowId(
            animePlatform.anime!!.countryCode!!.locale,
            animePlatform.platformId!!
        )

        val identifiers = episodes.flatMap { episode ->
            audioLocales.map { audioLocale ->
                StringUtils.getIdentifier(
                    animePlatform.anime!!.countryCode!!,
                    animePlatform.platform!!,
                    episode.id,
                    audioLocale
                )
            }
        }.toSet()

        val show = episodes.first().show

        require(identifiers.isNotEmpty()) { "No episode found for Prime Video anime ${show.name}" }

        return UpdatableAnime(
            platform = Platform.PRIM,
            attachments = buildMap { put(ImageType.BANNER, show.banner) },
            description = show.description,
            identifiers = identifiers,
        )
    }
}