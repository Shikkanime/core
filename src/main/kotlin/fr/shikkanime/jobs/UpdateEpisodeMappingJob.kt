package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.*
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper.BrowseObject
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.*
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

class UpdateEpisodeMappingJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val availableUpdatePlatforms = listOf(Platform.ANIM, Platform.CRUN, Platform.DISN, Platform.NETF, Platform.PRIM)

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var languageCacheService: LanguageCacheService

    @Inject
    private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform

    @Inject
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

    @Inject
    private lateinit var disneyPlusPlatform: DisneyPlusPlatform

    @Inject
    private lateinit var netflixPlatform: NetflixPlatform

    @Inject
    private lateinit var primeVideoPlatform: PrimeVideoPlatform

    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var mailService: MailService

    override fun run() {
        val lastDateTime = ZonedDateTime.now().minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY, 30).toLong())

        val allPlatformEpisodes = episodeMappingService.findAllNeedUpdateByPlatforms(availableUpdatePlatforms, lastDateTime)
        logger.info("Found ${allPlatformEpisodes.size} episodes to update")

        val needUpdateEpisodes = allPlatformEpisodes
            .shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_SIZE, 15))

        if (needUpdateEpisodes.isEmpty()) {
            logger.info("No episode to update")
            return
        }

        val needRecalculate = AtomicBoolean(false)
        val needRefreshCache = AtomicBoolean(false)
        val identifiers = episodeVariantCacheService.findAllIdentifiers().toMutableSet()
        val allPreviousAndNext = mutableListOf<Episode>()

        needUpdateEpisodes.forEach { mapping ->
            val variants = episodeVariantService.findAllByMapping(mapping)
            val mappingIdentifier = "${StringUtils.getShortName(mapping.anime!!.name!!)} - S${mapping.season} ${mapping.episodeType} ${mapping.number}"
            logger.info("Updating episode $mappingIdentifier...")

            val tmpIdentifiers = identifiers.toMutableSet()

            val episodes = variants.flatMap { variant -> runBlocking { retrievePlatformEpisode(mapping, variant, lastDateTime, identifiers) } }
                .sortedBy { it.platform.sortIndex }

            if (tmpIdentifiers != identifiers) {
                needRefreshCache.set(true)
            }

            allPreviousAndNext.addAll(checkPreviousAndNextEpisodes(mapping.anime!!, variants))
            saveAnimePlatformIfNotExists(episodes, mapping)

            if (episodes.isEmpty()) {
                logger.warning("No episode found for $mappingIdentifier")
                mapping.lastUpdateDateTime = ZonedDateTime.now()
                episodeMappingService.update(mapping)
                logger.info("Episode $mappingIdentifier updated")
                return@forEach
            }

            episodes.distinctBy { it.getIdentifier() }
                .filter { it.getIdentifier() !in identifiers }
                .takeIf { it.isNotEmpty() }
                ?.also { logger.info("Found ${it.size} new episodes for $mappingIdentifier") }
                ?.map { episode -> episodeVariantService.save(episode, false, mapping) }
                ?.also { episodeVariants ->
                    identifiers.addAll(episodeVariants.mapNotNull { it.identifier })
                    logger.info("Added ${episodeVariants.size} episodes for $mappingIdentifier")
                    needRecalculate.set(true)
                    needRefreshCache.set(true)
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

        if (allPreviousAndNext.isNotEmpty()) {
            val mappingUuids = episodeMappingService.findAllUuids()

            allPreviousAndNext.distinctBy { it.getIdentifier() }
                .filter { it.getIdentifier() !in identifiers }
                .takeIf { it.isNotEmpty() }
                ?.also { logger.info("Found ${it.size} new previous and next episodes") }
                ?.map { episode -> episodeVariantService.save(episode, false) }
                ?.toMutableList()
                ?.also { episodeVariants ->
                    episodeVariants.removeIf { it.mapping!!.uuid in mappingUuids }

                    identifiers.addAll(episodeVariants.mapNotNull { it.identifier })
                    allNewEpisodes.addAll(episodeVariants)
                    logger.info("Added ${episodeVariants.size} previous and next episodes")
                    needRecalculate.set(true)
                    needRefreshCache.set(true)

                    episodeVariants.forEach {
                        it.mapping!!.lastUpdateDateTime = ZonedDateTime.parse("2000-01-01T00:00:00Z")
                        episodeMappingService.update(it.mapping!!)
                    }
                }
        }

        if (needRecalculate.get()) {
            logger.info("Recalculating simulcasts...")
            animeService.recalculateSimulcasts()
        }

        logger.info("Episodes updated")

        if (needRefreshCache.get()) {
            MapCache.invalidate(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                Simulcast::class.java
            )
        }

        if (allNewEpisodes.isNotEmpty()) {
            val dtos = AbstractConverter.convert(allNewEpisodes.mapNotNull { it.mapping }.distinctBy { it.uuid }, EpisodeMappingDto::class.java)!!

            logger.info("New episodes:")
            val lines = mutableSetOf<String>()

            dtos.forEach {
                val line = "${it.anime.shortName} | ${StringUtils.toEpisodeMappingString(it)}"
                lines.add(line)
                logger.info("- $line")
            }

            try {
                mailService.save(
                    Mail(
                        recipient = configCacheService.getValueAsString(ConfigPropertyKey.ADMIN_EMAIL),
                        title = "UpdateEpisodeMappingJob - ${dtos.size} new episodes",
                        body = lines.joinToString("<br>") { "- $it" }
                    )
                )
            } catch (e: Exception) {
                logger.warning("Error while sending mail for UpdateEpisodeMappingJob: ${e.message}")
            }
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
        val platformIds = mutableMapOf<String, Platform>()

        variants.forEach { variant ->
            val identifier = when (variant.platform) {
                Platform.ANIM -> animationDigitalNetworkPlatform.getAnimationDigitalNetworkId(variant.identifier!!)!!
                Platform.CRUN -> crunchyrollPlatform.getCrunchyrollId(variant.identifier!!)!!
                else -> return@forEach
            }

            var previousId: String? = identifier
            var nextId: String? = identifier

            repeat(configCacheService.getValueAsInt(ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH, 1)) {
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
                        else -> emptyList()
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
        episodeVariant: EpisodeVariant,
        lastDateTime: ZonedDateTime,
        identifiers: MutableSet<String>
    ): List<Episode> {
        val countryCode = episodeMapping.anime!!.countryCode!!
        val episodes = mutableListOf<Episode>()
        val isImageUpdate = episodeMapping.image == Constant.DEFAULT_IMAGE_PREVIEW && episodeMapping.releaseDateTime >= lastDateTime

        fun updateIdentifiers(oldId: String, newId: String) {
            identifiers.remove(oldId)
            identifiers.add(newId)
        }

        when (episodeVariant.platform) {
            Platform.ANIM -> runCatching {
                val videoId = animationDigitalNetworkPlatform.getAnimationDigitalNetworkId(episodeVariant.identifier!!)!!.toInt()
                episodes.addAll(animationDigitalNetworkPlatform.convertEpisode(
                    countryCode,
                    AnimationDigitalNetworkCachedWrapper.getVideo(videoId),
                    ZonedDateTime.now(),
                    needSimulcast = false,
                    checkAnimation = false
                ))
            }

            Platform.CRUN -> runCatching {
                episodes.addAll(getCrunchyrollEpisodeAndVariants(
                    countryCode,
                    crunchyrollPlatform.getCrunchyrollId(episodeVariant.identifier!!)!!,
                    isImageUpdate
                ))
            }

            Platform.DISN -> runCatching {
                val disneyPlusId = disneyPlusPlatform.getDisneyPlusId(episodeVariant.identifier!!)!!
                val playerVideo = DisneyPlusCachedWrapper.getShowIdByEpisodeId(disneyPlusId)

                if (playerVideo.id != disneyPlusId) {
                    val oldIdentifier = episodeVariant.identifier!!
                    logger.warning("Updating Disney+ episode $disneyPlusId to ${playerVideo.id}")
                    episodeVariant.identifier = oldIdentifier.replace(disneyPlusId, playerVideo.id)
                    episodeVariantService.update(episodeVariant)
                    updateIdentifiers(oldIdentifier, episodeVariant.identifier!!)
                }

                val disneyPlusEpisodes = DisneyPlusCachedWrapper.getEpisodesByShowId(countryCode.locale, playerVideo.showId)
                disneyPlusEpisodes.forEach { episode ->
                    val convertedEpisode = disneyPlusPlatform.convertEpisode(
                        countryCode,
                        episode,
                        episodeVariant.releaseDateTime
                    )

                    if (convertedEpisode.getIdentifier() == episodeVariant.identifier) {
                        episodes.add(convertedEpisode)
                        if (convertedEpisode.url != episodeVariant.url) {
                            episodeVariant.url = convertedEpisode.url
                            episodeVariantService.update(episodeVariant)
                        }

                        DisneyPlusCachedWrapper.getAudioLocales(playerVideo.resourceId)
                            .filter { LangType.fromAudioLocale(countryCode, it) == LangType.VOICE }
                            .forEach { audioLocale ->
                                episodes.add(disneyPlusPlatform.convertEpisode(
                                    countryCode,
                                    episode,
                                    episodeVariant.releaseDateTime,
                                    audioLocale = audioLocale,
                                    original = false
                                ))
                            }
                    }
                }
            }

            Platform.NETF -> runCatching {
                val showId = netflixPlatform.getShowId(episodeVariant.url!!) ?: return emptyList()
                val netflixEpisodes = NetflixCachedWrapper.getShowVideos(countryCode, showId)
                netflixEpisodes.map { episode ->
                    netflixPlatform.convertEpisode(
                        countryCode,
                        episodeMapping.anime!!.image!!,
                        episode,
                        ZonedDateTime.now(),
                        episodeMapping.episodeType!!,
                        episodeVariant.audioLocale!!
                    )
                }.find { it.getIdentifier() == episodeVariant.identifier }?.also { episodes.add(it) }
            }

            Platform.PRIM -> runCatching {
                val showId = primeVideoPlatform.getShowId(episodeVariant.url!!) ?: return emptyList()
                val primeVideoEpisodes = PrimeVideoCachedWrapper.getShowVideos(countryCode, showId)
                primeVideoEpisodes.map { episode ->
                    primeVideoPlatform.convertEpisode(
                        countryCode,
                        episodeMapping.anime!!.image!!,
                        episode,
                        ZonedDateTime.now()
                    )
                }.find { it.getIdentifier() == episodeVariant.identifier }?.also { episodes.add(it) }
            }

            else -> logger.warning("Error while getting episode ${episodeVariant.identifier} : Invalid platform")
        }

        return episodes
    }

    private suspend fun getCrunchyrollEpisodeAndVariants(
        countryCode: CountryCode,
        crunchyrollId: String,
        isImageUpdate: Boolean
    ): List<Episode> {
        val browseObjects = mutableListOf<BrowseObject>()

        val variantObjects = (
                if (isImageUpdate)
                    CrunchyrollWrapper.getEpisode(
                        countryCode.locale,
                        crunchyrollId
                    )
                else
                    CrunchyrollCachedWrapper.getEpisode(
                        countryCode.locale,
                        crunchyrollId
                    )
                ).also { browseObjects.add(it.convertToBrowseObject()) }
            .getVariants()
            .subtract(browseObjects.map { it.id }.toSet())
            .chunked(AbstractCrunchyrollWrapper.CRUNCHYROLL_CHUNK)
            .flatMap { chunk ->
                HttpRequest.retry(3) {
                    CrunchyrollCachedWrapper.getObjects(
                        countryCode.locale,
                        *chunk.toTypedArray()
                    )
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
            Platform.ANIM -> runCatching {
                AnimationDigitalNetworkCachedWrapper.getPreviousVideo(
                    AnimationDigitalNetworkCachedWrapper.getVideo(id.toInt()).show.id,
                    id.toInt()
                )?.id?.toString()
            }.onFailure {
                logger.warning("Error while getting previous episode for $id: ${it.message}")
            }.getOrNull()

            Platform.CRUN -> runCatching {
                CrunchyrollCachedWrapper.getPreviousEpisode(countryCode.locale, id).id
            }.onFailure {
                logger.warning("Error while getting previous episode for $id: ${it.message}")
            }.getOrNull()

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
            Platform.ANIM -> runCatching {
                AnimationDigitalNetworkCachedWrapper.getNextVideo(
                    AnimationDigitalNetworkCachedWrapper.getVideo(id.toInt()).show.id,
                    id.toInt()
                )?.id?.toString()
            }.onFailure {
                logger.warning("Error while getting next episode for $id: ${it.message}")
            }.getOrNull()

            Platform.CRUN -> runCatching {
                CrunchyrollCachedWrapper.getUpNext(countryCode.locale, id).id
            }.onFailure {
                logger.warning("Error while getting next episode for $id: ${it.message}")
            }.getOrNull()

            else -> {
                logger.warning("Error while getting next episode $id: Invalid platform")
                null
            }
        }
    }
}