package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper.BrowseObject
import fr.shikkanime.wrappers.CrunchyrollWrapper.getObjects
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

class UpdateEpisodeMappingJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var languageCacheService: LanguageCacheService

    @Inject
    private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform

    @Inject
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var emailService: EmailService

    private val adnCache = MapCache<CountryCodeIdKeyCache, List<Episode>>(duration = Duration.ofDays(1)) {
        return@MapCache runBlocking {
            val video = try {
                AnimationDigitalNetworkWrapper.getVideo(it.id.toInt())
            } catch (e: Exception) {
                logger.severe("Impossible to get ADN video ${it.id} : ${e.message} (Maybe the video is not available anymore)")
                return@runBlocking emptyList()
            }

            try {
                animationDigitalNetworkPlatform.convertEpisode(
                    it.countryCode,
                    video,
                    ZonedDateTime.now(),
                    needSimulcast = false,
                    checkAnimation = false
                )
            } catch (e: Exception) {
                logger.warning("Error while getting ADN episode ${it.id} : ${e.message}")
                emptyList()
            }
        }
    }

    override fun run() {
        val lastDateTime = ZonedDateTime.now().minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY, 30).toLong())
        val adnEpisodes = episodeMappingService.findAllNeedUpdateByPlatform(Platform.ANIM, lastDateTime)
        val crunchyrollEpisodes = episodeMappingService.findAllNeedUpdateByPlatform(Platform.CRUN, lastDateTime)

        logger.info("Found ${adnEpisodes.size} ADN episodes and ${crunchyrollEpisodes.size} Crunchyroll episodes to update")

        val needUpdateEpisodes = (adnEpisodes + crunchyrollEpisodes).distinctBy { it.uuid }
            .shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_SIZE, 15))

        if (needUpdateEpisodes.isEmpty()) {
            logger.info("No episode to update")
            return
        }

        val needRecalculate = AtomicBoolean(false)
        val needRefreshCache = AtomicBoolean(false)
        val identifiers = episodeVariantService.findAllIdentifiers().toMutableSet()

        val allPrevious = mutableListOf<Episode>()
        val allNext = mutableListOf<Episode>()

        needUpdateEpisodes.forEach { mapping ->
            val variants = episodeVariantService.findAllByMapping(mapping)
            val mappingIdentifier = "${StringUtils.getShortName(mapping.anime!!.name!!)} - S${mapping.season} ${mapping.episodeType} ${mapping.number}"
            logger.info("Updating episode $mappingIdentifier...")

            val episodes = variants.flatMap { variant -> runBlocking { retrievePlatformEpisode(mapping, variant) } }
                .sortedBy { it.platform.sortIndex }

            variants.map { variant -> runBlocking { retrievePreviousAndNextEpisodes(mapping, variant) } }
                .forEach { (previous, next) ->
                    allPrevious.addAll(previous.filter { it.episodeType == EpisodeType.EPISODE })
                    allNext.addAll(next.filter { it.episodeType == EpisodeType.EPISODE })
                }

            saveAnimePlatformIfNotExists(episodes, mapping)

            if (episodes.isEmpty()) {
                logger.warning("No episode found for $mappingIdentifier")
                mapping.lastUpdateDateTime = ZonedDateTime.now()
                episodeMappingService.update(mapping)
                logger.info("Episode $mappingIdentifier updated")
                return@forEach
            }

            episodes.filter { it.getIdentifier() !in identifiers }.takeIf { it.isNotEmpty() }
                ?.distinctBy { it.getIdentifier() }
                ?.also { logger.info("Found ${it.size} new episodes for $mappingIdentifier") }
                ?.map { episode -> episodeVariantService.save(episode, false, mapping) }
                ?.also {
                    identifiers.addAll(it.mapNotNull { it.identifier })
                    logger.info("Added ${it.size} episodes for $mappingIdentifier")
                    needRecalculate.set(true)
                    needRefreshCache.set(true)
                }

            episodeVariantService.findAllByMapping(mapping).forEach { episodeVariant ->
                if (episodes.none { it.getIdentifier() == episodeVariant.identifier }) {
                    episodeVariantService.delete(episodeVariant)
                    logger.info("Deleted episode ${episodeVariant.identifier} for $mappingIdentifier")
                    needRecalculate.set(true)
                    needRefreshCache.set(true)
                }
            }

            val originalEpisode = episodes.firstOrNull { it.original } ?: episodes.first()
            val hasChanged = AtomicBoolean(false)

            updateEpisodeMappingImage(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)
            updateEpisodeMappingTitle(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)
            updateEpisodeMappingDescription(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)
            updateEpisodeMappingDuration(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)

            mapping.status = StringUtils.getStatus(mapping)
            mapping.lastUpdateDateTime = ZonedDateTime.now()
            episodeMappingService.update(mapping)

            if (hasChanged.get()) {
                traceActionService.createTraceAction(mapping, TraceAction.Action.UPDATE)
            }

            logger.info("Episode $mappingIdentifier updated")
        }

        val allNewEpisodes = mutableSetOf<EpisodeVariant>()

        allPrevious.distinctBy { it.getIdentifier() }
            .filter { it.getIdentifier() !in identifiers }
            .takeIf { it.isNotEmpty() }
            ?.also { logger.info("Found ${it.size} new previous episodes") }
            ?.map { episode -> episodeVariantService.save(episode, false, null) }
            ?.also {
                identifiers.addAll(it.mapNotNull { it.identifier })
                allNewEpisodes.addAll(it)
                logger.info("Added ${it.size} previous episodes")
                needRecalculate.set(true)
                needRefreshCache.set(true)
            }

        allNext.distinctBy { it.getIdentifier() }
            .filter { it.getIdentifier() !in identifiers }
            .takeIf { it.isNotEmpty() }
            ?.also { logger.info("Found ${it.size} new next episodes") }
            ?.map { episode -> episodeVariantService.save(episode, false, null) }
            ?.also {
                identifiers.addAll(it.mapNotNull { it.identifier })
                allNewEpisodes.addAll(it)
                logger.info("Added ${it.size} next episodes")
                needRecalculate.set(true)
                needRefreshCache.set(true)
            }

        if (needRecalculate.get()) {
            logger.info("Recalculating simulcasts...")
            animeService.recalculateSimulcasts()
        }

        logger.info("Episodes updated")

        if (needRefreshCache.get()) {
            MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java, Simulcast::class.java)
        }

        if (allNewEpisodes.isNotEmpty()) {
            val dtos = AbstractConverter.convert(allNewEpisodes, EpisodeVariantDto::class.java)!!
            val body = dtos.joinToString("\n") { "- ${it.mapping.anime.shortName} | ${StringUtils.toEpisodeString(it)}" }

            logger.info("New episodes:")
            logger.info(body)

            emailService.sendAdminEmail(
                "UpdateEpisodeMappingJob - ${allNewEpisodes.size} new episodes",
                body
            )
        }
    }

    private fun updateEpisodeMappingImage(
        originalEpisode: Episode,
        mapping: EpisodeMapping,
        mappingIdentifier: String,
        hasChanged: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        if (originalEpisode.image != Constant.DEFAULT_IMAGE_PREVIEW && mapping.image != originalEpisode.image) {
            mapping.image = originalEpisode.image
            episodeMappingService.addImage(mapping.uuid!!, originalEpisode.image, true)
            logger.info("Image updated for $mappingIdentifier to ${originalEpisode.image}")
            hasChanged.set(true)
            needRefreshCache.set(true)
        }
    }

    private fun updateEpisodeMappingTitle(
        originalEpisode: Episode,
        mapping: EpisodeMapping,
        mappingIdentifier: String,
        hasChanged: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        if (originalEpisode.title.normalize() != mapping.title && !originalEpisode.title.isNullOrBlank()) {
            mapping.title = originalEpisode.title.normalize()
            logger.info("Title updated for $mappingIdentifier to ${originalEpisode.title.normalize()}")
            hasChanged.set(true)
            needRefreshCache.set(true)
        }
    }

    private fun updateEpisodeMappingDescription(
        originalEpisode: Episode,
        mapping: EpisodeMapping,
        mappingIdentifier: String,
        hasChanged: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        val trimmedDescription = originalEpisode.description?.take(Constant.MAX_DESCRIPTION_LENGTH).normalize()

        if (trimmedDescription != mapping.description &&
            !trimmedDescription.isNullOrBlank() &&
            languageCacheService.detectLanguage(trimmedDescription) == mapping.anime!!.countryCode!!.name.lowercase()
        ) {
            mapping.description = trimmedDescription
            logger.info("Description updated for $mappingIdentifier to $trimmedDescription")
            hasChanged.set(true)
            needRefreshCache.set(true)
        }
    }

    private fun updateEpisodeMappingDuration(
        originalEpisode: Episode,
        mapping: EpisodeMapping,
        mappingIdentifier: String,
        hasChanged: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        if (originalEpisode.duration != mapping.duration) {
            mapping.duration = originalEpisode.duration
            logger.info("Duration updated for $mappingIdentifier to ${originalEpisode.duration}")
            hasChanged.set(true)
            needRefreshCache.set(true)
        }
    }

    private fun saveAnimePlatformIfNotExists(
        episodes: List<Episode>,
        mapping: EpisodeMapping
    ) {
        episodes.forEach {
            if (animePlatformService.findByAnimePlatformAndId(mapping.anime!!, it.platform, it.animeId) == null) {
                animePlatformService.save(
                    AnimePlatform(
                        anime = mapping.anime,
                        platform = it.platform,
                        platformId = it.animeId
                    )
                )
            }
        }
    }

    private suspend fun retrievePlatformEpisode(
        episodeMapping: EpisodeMapping,
        episodeVariant: EpisodeVariant
    ): List<Episode> {
        val countryCode = episodeMapping.anime!!.countryCode!!
        val episodes = mutableListOf<Episode>()

        when (episodeVariant.platform) {
            Platform.ANIM -> {
                episodes.addAll(
                    adnCache[CountryCodeIdKeyCache(countryCode, animationDigitalNetworkPlatform.getAnimationDigitalNetworkId(episodeVariant.identifier!!)!!)]!!
                )
            }

            Platform.CRUN -> {
                episodes.addAll(
                    getCrunchyrollEpisodeAndVariants(
                        countryCode,
                        crunchyrollPlatform.getCrunchyrollId(episodeVariant.identifier!!)!!
                    )
                )
            }

            else -> {
                logger.warning("Error while getting episode ${episodeVariant.identifier} : Invalid platform")
            }
        }

        return episodes
    }

    private suspend fun getCrunchyrollEpisodeAndVariants(
        countryCode: CountryCode,
        crunchyrollId: String,
    ): List<Episode> {
        val browseObjects = mutableListOf<BrowseObject>()

        val variantObjects = CrunchyrollWrapper.getEpisode(
            countryCode.locale,
            CrunchyrollWrapper.getAccessTokenCached(countryCode)!!,
            crunchyrollId
        )
            .also { browseObjects.add(it.convertToBrowseObject()) }
            .getVariants()
            .subtract(browseObjects.map { it.id }.toSet())
            .chunked(CrunchyrollWrapper.CRUNCHYROLL_CHUNK)
            .flatMap { chunk ->
                HttpRequest.retry(3) {
                    getObjects(
                        countryCode.locale,
                        CrunchyrollWrapper.getAccessTokenCached(countryCode)!!,
                        *chunk.toTypedArray()
                    ).toList()
                }
            }

        return (browseObjects + variantObjects).mapNotNull { browseObject ->
            try {
                crunchyrollPlatform.convertEpisode(
                    countryCode,
                    browseObject,
                    needSimulcast = false,
                )
            } catch (e: Exception) {
                logger.warning("Error while getting Crunchyroll episode ${browseObject.id} : ${e.message}")
                return@mapNotNull null
            }
        }
    }

    private suspend fun retrievePreviousAndNextEpisodes(
        episodeMapping: EpisodeMapping,
        episodeVariant: EpisodeVariant
    ): Pair<List<Episode>, List<Episode>> {
        val countryCode = episodeMapping.anime!!.countryCode!!
        val previous = mutableListOf<Episode>()
        val next = mutableListOf<Episode>()

        when (episodeVariant.platform) {
            Platform.ANIM -> {
                val videoId = animationDigitalNetworkPlatform.getAnimationDigitalNetworkId(episodeVariant.identifier!!)!!
                val video = adnCache[CountryCodeIdKeyCache(countryCode, videoId)]!!.first()

                AnimationDigitalNetworkWrapper.getPreviousVideo(videoId.toInt(), video.animeId.toInt())
                    ?.let { previous.addAll(animationDigitalNetworkPlatform.convertEpisode(countryCode, it, ZonedDateTime.now(), needSimulcast = false, checkAnimation = false)) }

                AnimationDigitalNetworkWrapper.getNextVideo(videoId.toInt(), video.animeId.toInt())
                    ?.let { next.addAll(animationDigitalNetworkPlatform.convertEpisode(countryCode, it, ZonedDateTime.now(), needSimulcast = false, checkAnimation = false)) }
            }

            Platform.CRUN -> {
                val videoId = crunchyrollPlatform.getCrunchyrollId(episodeVariant.identifier!!)!!

                runCatching {
                    CrunchyrollWrapper.getPreviousEpisode(countryCode.locale, CrunchyrollWrapper.getAccessTokenCached(countryCode)!!, videoId)
                        .let { previous.add(crunchyrollPlatform.convertEpisode(countryCode, it, needSimulcast = false)) }
                }.onFailure {
                    logger.warning("Error while getting previous episode for ${episodeVariant.identifier} : ${it.message}")
                }

                runCatching {
                    CrunchyrollWrapper.getUpNext(countryCode.locale, CrunchyrollWrapper.getAccessTokenCached(countryCode)!!, videoId)
                        .let { next.add(crunchyrollPlatform.convertEpisode(countryCode, it, needSimulcast = false)) }
                }.onFailure {
                    logger.warning("Error while getting next episode for ${episodeVariant.identifier} : ${it.message}")
                }
            }

            else -> {
                logger.warning("Error while getting episode ${episodeVariant.identifier} : Invalid platform")
            }
        }

        return previous to next
    }
}