package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
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
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper.BrowseObject
import fr.shikkanime.wrappers.impl.caches.AnimationDigitalNetworkCachedWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import kotlinx.coroutines.runBlocking
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

        val allPreviousAndNext = mutableListOf<Episode>()

        needUpdateEpisodes.forEach { mapping ->
            val variants = episodeVariantService.findAllByMapping(mapping)
            val mappingIdentifier = "${StringUtils.getShortName(mapping.anime!!.name!!)} - S${mapping.season} ${mapping.episodeType} ${mapping.number}"
            logger.info("Updating episode $mappingIdentifier...")

            val episodes = variants.flatMap { variant -> runBlocking { retrievePlatformEpisode(mapping, variant) } }
                .sortedBy { it.platform.sortIndex }

            allPreviousAndNext.addAll(checkPreviousAndNextEpisodes(mapping.anime!!, variants))
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

        allPreviousAndNext.distinctBy { it.getIdentifier() }
            .filter { it.getIdentifier() !in identifiers }
            .takeIf { it.isNotEmpty() }
            ?.also { logger.info("Found ${it.size} new previous and next episodes") }
            ?.map { episode -> episodeVariantService.save(episode, false, null) }
            ?.also {
                identifiers.addAll(it.mapNotNull { it.identifier })
                allNewEpisodes.addAll(it)
                logger.info("Added ${it.size} previous and next episodes")
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
            val dtos = AbstractConverter.convert(allNewEpisodes.mapNotNull { it.mapping }.distinctBy { it.uuid }, EpisodeMappingDto::class.java)!!

            logger.info("New episodes:")

            dtos.forEach {
                logger.info("${it.anime.shortName} | Saison ${it.season} • ${StringUtils.getEpisodeTypeLabel(it.episodeType)} ${it.number}")
            }

            emailService.sendAdminEmail(
                "UpdateEpisodeMappingJob - ${allNewEpisodes.size} new episodes",
                dtos.joinToString("<br>") { "- ${it.anime.shortName} | Saison ${it.season} • ${StringUtils.getEpisodeTypeLabel(it.episodeType)} ${it.number}" }
            )
        }
    }

    private fun checkPreviousAndNextEpisodes(
        anime: Anime,
        variants: List<EpisodeVariant>,
    ): List<Episode> {
        val checkPreviousAndNextEpisodes = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_PREVIOUS_AND_NEXT_EPISODES)

        if (!checkPreviousAndNextEpisodes) {
            return emptyList()
        }

        val countryCode = anime.countryCode!!
        val depth = configCacheService.getValueAsInt(ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH, 1)
        val platformIds = mutableMapOf<String, Platform>()

        variants.forEach { variant ->
            val identifier = when (variant.platform) {
                Platform.ANIM -> animationDigitalNetworkPlatform.getAnimationDigitalNetworkId(variant.identifier!!)!!
                Platform.CRUN -> crunchyrollPlatform.getCrunchyrollId(variant.identifier!!)!!
                else -> return@forEach
            }

            var previousId: String? = identifier
            var nextId: String? = identifier

            repeat(depth) {
                runBlocking {
                    previousId = previousId?.let { retrievePreviousEpisodes(countryCode, variant.platform!!, it) }
                    nextId = nextId?.let { retrieveNextEpisodes(countryCode, variant.platform!!, it) }

                    previousId?.let { platformIds[it] = variant.platform!! }
                    nextId?.let { platformIds[it] = variant.platform!! }
                }
            }
        }

        return platformIds.flatMap { (id, platform) ->
            runCatching {
                runBlocking {
                    when (platform) {
                        Platform.ANIM -> animationDigitalNetworkPlatform.convertEpisode(countryCode, AnimationDigitalNetworkCachedWrapper.getVideo(id.toInt()), ZonedDateTime.now(), needSimulcast = false, checkAnimation = false)
                        Platform.CRUN -> listOf(crunchyrollPlatform.convertEpisode(countryCode, CrunchyrollCachedWrapper.getObjects(countryCode.locale, id).first(), needSimulcast = false))
                        else -> emptyList<Episode>()
                    }
                }
            }.onFailure {
                logger.warning("Error while getting previous and next episodes for $id: ${it.message}")
            }.getOrDefault(emptyList())
        }.distinctBy { it.getIdentifier() }.filter { it.episodeType == EpisodeType.EPISODE }
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
                    animationDigitalNetworkPlatform.convertEpisode(
                        countryCode,
                        AnimationDigitalNetworkCachedWrapper.getVideo(
                            animationDigitalNetworkPlatform.getAnimationDigitalNetworkId(episodeVariant.identifier!!)!!.toInt()
                        ),
                        ZonedDateTime.now(),
                        needSimulcast = false,
                        checkAnimation = false
                    )
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

        val variantObjects = CrunchyrollCachedWrapper.getEpisode(
            countryCode.locale,
            crunchyrollId
        )
            .also { browseObjects.add(it.convertToBrowseObject()) }
            .getVariants()
            .subtract(browseObjects.map { it.id }.toSet())
            .chunked(AbstractCrunchyrollWrapper.CRUNCHYROLL_CHUNK)
            .flatMap { chunk ->
                HttpRequest.retry(3) {
                    CrunchyrollCachedWrapper.getObjects(
                        countryCode.locale,
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

    private suspend fun retrievePreviousEpisodes(
        countryCode: CountryCode,
        platform: Platform,
        id: String,
    ): String? {
        return when (platform) {
            Platform.ANIM -> retrievePreviousADNEpisodes(id)
            Platform.CRUN -> retrievePreviousCrunchyrollEpisodes(countryCode, id)
            else -> {
                logger.warning("Error while getting previous episode $id: Invalid platform")
                null
            }
        }
    }


    private suspend fun retrieveNextEpisodes(
        countryCode: CountryCode,
        platform: Platform,
        id: String,
    ): String? {
        return when (platform) {
            Platform.ANIM -> retrieveNextADNEpisodes(id)
            Platform.CRUN -> retrieveNextCrunchyrollEpisodes(countryCode, id)
            else -> {
                logger.warning("Error while getting next episode $id: Invalid platform")
                null
            }
        }
    }

    private suspend fun retrievePreviousADNEpisodes(
        id: String,
    ) = runCatching {
        AnimationDigitalNetworkCachedWrapper.getPreviousVideo(
            AnimationDigitalNetworkCachedWrapper.getVideo(id.toInt()).show.id,
            id.toInt()
        )?.id?.toString()
    }.onFailure {
        logger.warning("Error while getting previous episode for $id: ${it.message}")
    }.getOrNull()

    private suspend fun retrieveNextADNEpisodes(
        id: String,
    ) = runCatching {
        AnimationDigitalNetworkCachedWrapper.getNextVideo(
            AnimationDigitalNetworkCachedWrapper.getVideo(id.toInt()).show.id,
            id.toInt()
        )?.id?.toString()
    }.onFailure {
        logger.warning("Error while getting next episode for $id: ${it.message}")
    }.getOrNull()

    private suspend fun retrievePreviousCrunchyrollEpisodes(
        countryCode: CountryCode,
        id: String
    ) = runCatching {
        CrunchyrollCachedWrapper.getPreviousEpisode(countryCode.locale, id).id
    }.onFailure {
        logger.warning("Error while getting previous episode for $id: ${it.message}")
    }.getOrNull()

    private suspend fun retrieveNextCrunchyrollEpisodes(
        countryCode: CountryCode,
        id: String
    ) = runCatching {
        CrunchyrollCachedWrapper.getUpNext(countryCode.locale, id).id
    }.onFailure {
        logger.warning("Error while getting next episode for $id: ${it.message}")
    }.getOrNull()
}