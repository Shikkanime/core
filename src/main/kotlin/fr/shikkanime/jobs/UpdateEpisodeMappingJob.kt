package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.*
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
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

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform
    @Inject private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @Inject private lateinit var disneyPlusPlatform: DisneyPlusPlatform
    @Inject private lateinit var netflixPlatform: NetflixPlatform
    @Inject private lateinit var primeVideoPlatform: PrimeVideoPlatform
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var mailService: MailService
    @Inject private lateinit var attachmentService: AttachmentService

    override fun run() {
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val lastUpdateDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY, 30).toLong())
        val lastImageUpdateDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_EPISODE_DELAY, 2).toLong())

        val allPlatformEpisodes = episodeMappingService.findAllNeedUpdate(lastUpdateDateTime, lastImageUpdateDateTime)
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
        val identifiers = episodeVariantService.findAllIdentifiers()
        val allPreviousAndNext = mutableListOf<Episode>()

        needUpdateEpisodes.forEach { mapping ->
            updateEpisodeMapping(mapping, identifiers, allPreviousAndNext, lastImageUpdateDateTime, zonedDateTime, needRecalculate, needRefreshCache)
        }

        processNewEpisodes(allPreviousAndNext, identifiers, needRecalculate, needRefreshCache)

        if (needRecalculate.get()) {
            logger.info("Recalculating simulcasts...")
            animeService.recalculateSimulcasts()
            episodeVariantService.preIndex()
        }

        logger.info("Episodes updated")

        if (needRefreshCache.get()) {
            InvalidationService.invalidate(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                Simulcast::class.java
            )
        }
    }

    private fun updateEpisodeMapping(
        mapping: EpisodeMapping,
        identifiers: HashSet<String>,
        allPreviousAndNext: MutableList<Episode>,
        lastImageUpdateDateTime: ZonedDateTime,
        zonedDateTime: ZonedDateTime,
        needRecalculate: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        val variants = episodeVariantService.findAllByMapping(mapping)
        val mappingIdentifier = "${StringUtils.getShortName(mapping.anime!!.name!!)} ${StringUtils.toEpisodeMappingString(mapping)}"
        logger.info("Updating episode $mappingIdentifier...")

        val tmpIdentifiers = identifiers.toHashSet()

        val episodes = variants.flatMap { variant -> runBlocking { retrievePlatformEpisode(mapping, variant, lastImageUpdateDateTime, identifiers) } }
            .sortedBy { it.platform.sortIndex }

        if (tmpIdentifiers != identifiers) {
            needRefreshCache.set(true)
        }

        allPreviousAndNext.addAll(checkPreviousAndNextEpisodes(mapping.anime!!, variants))
        saveAnimePlatformIfNotExists(episodes, mapping)

        if (episodes.isEmpty()) {
            logger.warning("No episode found for $mappingIdentifier")
            mapping.lastUpdateDateTime = zonedDateTime
            episodeMappingService.update(mapping)
            logger.info("Episode $mappingIdentifier updated")
            return
        }

        var forceUpdate = false

        val newEpisodes = episodes.distinctBy { it.getIdentifier() }
            .filter { it.getIdentifier() !in identifiers }
        
        if (newEpisodes.isNotEmpty()) {
            logger.info("Found ${newEpisodes.size} new episodes for $mappingIdentifier")
            val episodeVariants = newEpisodes.map { episode -> episodeVariantService.save(episode, false, mapping) }
            identifiers.addAll(episodeVariants.mapNotNull { it.identifier })
            logger.info("Added ${episodeVariants.size} episodes for $mappingIdentifier")
            needRecalculate.set(true)
            needRefreshCache.set(true)
            forceUpdate = true
        }

        val originalEpisode = episodes.firstOrNull { it.original } ?: episodes.first()
        val hasChanged = AtomicBoolean(false)

        updateEpisodeDetails(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)

        mapping.lastUpdateDateTime = if (forceUpdate) {
            Constant.oldLastUpdateDateTime
        } else {
            zonedDateTime
        }

        episodeMappingService.update(mapping)

        if (hasChanged.get()) {
            traceActionService.createTraceAction(mapping, TraceAction.Action.UPDATE)
        }

        logger.info("Episode $mappingIdentifier updated")
    }

    private fun updateEpisodeDetails(
        originalEpisode: Episode,
        mapping: EpisodeMapping,
        mappingIdentifier: String,
        hasChanged: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        updateEpisodeMappingImage(originalEpisode, mapping, mappingIdentifier)
        updateEpisodeMappingTitle(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)
        updateEpisodeMappingDescription(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)
        updateEpisodeMappingDuration(originalEpisode, mapping, mappingIdentifier, hasChanged, needRefreshCache)
    }

    private fun processNewEpisodes(
        allPreviousAndNext: List<Episode>,
        identifiers: HashSet<String>,
        needRecalculate: AtomicBoolean,
        needRefreshCache: AtomicBoolean
    ) {
        if (allPreviousAndNext.isEmpty()) return

        val mappingUuids = episodeMappingService.findAllUuids()
        val newPreviousNextEpisodes = allPreviousAndNext.distinctBy { it.getIdentifier() }
            .filter { it.getIdentifier() !in identifiers }
        
        if (newPreviousNextEpisodes.isEmpty()) return
        
        logger.info("Found ${newPreviousNextEpisodes.size} new previous and next episodes")
        val episodeVariants = newPreviousNextEpisodes.map { episode -> episodeVariantService.save(episode, false) }
            .filter { it.mapping!!.uuid !in mappingUuids }
            .toList()
        
        if (episodeVariants.isEmpty()) return
        
        identifiers.addAll(episodeVariants.mapNotNull { it.identifier })
        logger.info("Added ${episodeVariants.size} previous and next episodes")
        needRecalculate.set(true)
        needRefreshCache.set(true)

        episodeVariants.forEach {
            it.mapping!!.lastUpdateDateTime = Constant.oldLastUpdateDateTime
            episodeMappingService.update(it.mapping!!)
        }

        notifyAdminAboutNewEpisodes(episodeVariants)
    }

    private fun notifyAdminAboutNewEpisodes(episodeVariants: List<EpisodeVariant>) {
        if (episodeVariants.isEmpty()) return

        val newMappings = episodeVariants.mapNotNull { it.mapping }.distinctBy { it.uuid }
        logger.info("New episodes:")
        
        val lines = newMappings.map { mapping ->
            "- ${StringUtils.getShortName(mapping.anime!!.name!!)} ${StringUtils.toEpisodeMappingString(mapping)}"
        }.toSet()
        
        lines.forEach { logger.info(it) }

        try {
            mailService.save(
                Mail(
                    recipient = configCacheService.getValueAsString(ConfigPropertyKey.ADMIN_EMAIL),
                    title = "UpdateEpisodeMappingJob - ${newMappings.size} new episodes",
                    body = lines.joinToString("<br>")
                )
            )
        } catch (e: Exception) {
            logger.warning("Error while sending mail for UpdateEpisodeMappingJob: ${e.message}")
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
        val depth = configCacheService.getValueAsInt(ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH, 1)

        variants.forEach { variant ->
            val identifier = StringUtils.getVideoOldIdOrId(variant.identifier!!)
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
                        Platform.ANIM -> animationDigitalNetworkPlatform.convertEpisode(countryCode, AnimationDigitalNetworkCachedWrapper.getVideo(countryCode, id.toInt()), ZonedDateTime.now(), needSimulcast = false, checkAnimation = false)
                        Platform.CRUN -> listOf(crunchyrollPlatform.convertEpisode(countryCode, CrunchyrollCachedWrapper.getObjects(countryCode.locale, id).first(), needSimulcast = false))
                        else -> emptyList()
                    }
                }
            }.onFailure { logger.warning("Error while getting previous and next episodes for $id: ${it.message}") }
                .getOrDefault(emptyList())
        }.distinctBy { it.getIdentifier() }
    }

    private fun updateEpisodeMappingImage(
        originalEpisode: Episode,
        mapping: EpisodeMapping,
        mappingIdentifier: String
    ) {
        val url = originalEpisode.image

        if (attachmentService.findByEntityUuidTypeAndActive(mapping.uuid!!, ImageType.BANNER)?.url != url && url.isNotBlank() && originalEpisode.image != Constant.DEFAULT_IMAGE_PREVIEW) {
            attachmentService.createAttachmentOrMarkAsActive(mapping.uuid, ImageType.BANNER, url = url)
            logger.info("Image updated for $mappingIdentifier to ${originalEpisode.image}")
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

        if (trimmedDescription != mapping.description && !trimmedDescription.isNullOrBlank()) {
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
        val now = ZonedDateTime.now()

        animePlatformService.updateAll(
            episodes.map {
                animePlatformService.findByAnimePlatformAndId(mapping.anime!!, it.platform, it.animeId)
                    ?: animePlatformService.save(
                        AnimePlatform(
                            anime = mapping.anime,
                            platform = it.platform,
                            platformId = it.animeId
                        )
                    ).apply { traceActionService.createTraceAction(this, TraceAction.Action.CREATE) }
            }.distinctBy { it.platform to it.platformId }
                .onEach {
                    it.lastValidateDateTime = now
                    traceActionService.createTraceAction(it, TraceAction.Action.UPDATE)
                }
        )
    }

    private fun updateIdentifier(
        episodeVariant: EpisodeVariant,
        oldId: String,
        newId: String,
        identifiers: HashSet<String>
    ) {
        if (oldId == newId) {
            return
        }

        val oldIdentifier = episodeVariant.identifier
        logger.warning("Updating ${episodeVariant.platform!!.name} episode $oldId to $newId")
        episodeVariant.identifier = oldIdentifier!!.replace(oldId, newId)
        episodeVariantService.update(episodeVariant)
        identifiers.remove(oldIdentifier)
        identifiers.add(episodeVariant.identifier!!)
    }

    private fun updateUrl(
        episodeVariant: EpisodeVariant,
        url: String,
    ) {
        if (episodeVariant.url == url) {
            return
        }

        logger.warning("Updating ${episodeVariant.platform!!.name} episode ${episodeVariant.identifier} URL to $url")
        episodeVariant.url = url
        episodeVariantService.update(episodeVariant)
    }

    private suspend fun retrievePlatformEpisode(
        episodeMapping: EpisodeMapping,
        episodeVariant: EpisodeVariant,
        lastImageUpdateDateTime: ZonedDateTime,
        identifiers: HashSet<String>
    ): List<Episode> {
        val countryCode = episodeMapping.anime!!.countryCode!!
        val episodes = mutableListOf<Episode>()
        val isImageUpdate = attachmentService.findByEntityUuidTypeAndActive(episodeMapping.uuid!!, ImageType.BANNER)?.url == Constant.DEFAULT_IMAGE_PREVIEW && episodeMapping.releaseDateTime.isAfterOrEqual(lastImageUpdateDateTime)
        val releaseDateTime = episodeVariant.releaseDateTime
        val identifier = episodeVariant.identifier!!

        when (episodeVariant.platform) {
            Platform.ANIM -> retrieveADNEpisode(countryCode, identifier, episodes)
            Platform.CRUN -> retrieveCrunchyrollEpisode(countryCode, identifier, isImageUpdate, episodes)
            Platform.DISN -> retrieveDisneyEpisode(countryCode, episodeVariant, episodeMapping, identifiers, episodes, releaseDateTime)
            Platform.NETF -> retrieveNetflixEpisode(countryCode, episodeVariant, episodeMapping, identifiers, episodes)
            Platform.PRIM -> retrievePrimeEpisode(countryCode, episodeVariant, episodeMapping, identifiers, episodes, releaseDateTime)
            else -> logger.warning("Error while getting episode $identifier : Invalid platform")
        }

        return episodes
    }

    private suspend fun retrieveADNEpisode(
        countryCode: CountryCode,
        identifier: String,
        episodes: MutableList<Episode>
    ) {
        runCatching {
            val videoId = StringUtils.getVideoOldIdOrId(identifier)!!.toInt()
            val video = AnimationDigitalNetworkCachedWrapper.getVideo(countryCode, videoId)
            episodes.addAll(
                animationDigitalNetworkPlatform.convertEpisode(
                    countryCode, video, ZonedDateTime.now(),
                    needSimulcast = false, checkAnimation = false
                )
            )
        }
    }

    private suspend fun retrieveCrunchyrollEpisode(
        countryCode: CountryCode,
        identifier: String,
        isImageUpdate: Boolean,
        episodes: MutableList<Episode>
    ) {
        runCatching {
            val crunchyrollId = StringUtils.getVideoOldIdOrId(identifier)!!
            episodes.addAll(getCrunchyrollEpisodeAndVariants(countryCode, crunchyrollId, isImageUpdate))
        }
    }

    private suspend fun retrieveDisneyEpisode(
        countryCode: CountryCode,
        episodeVariant: EpisodeVariant,
        episodeMapping: EpisodeMapping,
        identifiers: HashSet<String>,
        episodes: MutableList<Episode>,
        releaseDateTime: ZonedDateTime
    ) {
        runCatching {
            val id = StringUtils.getVideoOldIdOrId(episodeVariant.identifier!!) ?: return
            val ids = animePlatformService.findAllIdByAnimeAndPlatform(episodeMapping.anime!!, episodeVariant.platform!!)
            val checkAudioLocales = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
            val platformEpisodes = ids.flatMap { DisneyPlusCachedWrapper.getEpisodesByShowId(countryCode, it, checkAudioLocales).toList() }
            val episode = platformEpisodes.find { it.id == id || it.oldId == id } ?: return

            updateIdentifier(episodeVariant, id, episode.id, identifiers)
            updateUrl(episodeVariant, episode.url)

            episodes.addAll(
                disneyPlusPlatform.convertEpisode(
                    countryCode,
                    episode,
                    releaseDateTime
                )
            )
        }
    }

    private suspend fun retrieveNetflixEpisode(
        countryCode: CountryCode,
        episodeVariant: EpisodeVariant,
        episodeMapping: EpisodeMapping,
        identifiers: HashSet<String>,
        episodes: MutableList<Episode>,
    ) {
        runCatching {
            val id = StringUtils.getVideoOldIdOrId(episodeVariant.identifier!!) ?: return
            val ids = animePlatformService.findAllIdByAnimeAndPlatform(episodeMapping.anime!!, episodeVariant.platform!!)
            val platformEpisodes = ids.flatMap { NetflixCachedWrapper.getEpisodesByShowId(countryCode, it.toInt()).toList() }
            val episode = platformEpisodes.find { it.id.toString() == id || it.oldId == id } ?: return

            updateIdentifier(episodeVariant, id, episode.id.toString(), identifiers)
            updateUrl(episodeVariant, episode.url)

            episodes.add(
                netflixPlatform.convertEpisode(
                    countryCode,
                    StringUtils.EMPTY_STRING,
                    episode,
                    episodeVariant.audioLocale!!
                )
            )
        }
    }

    private suspend fun retrievePrimeEpisode(
        countryCode: CountryCode,
        episodeVariant: EpisodeVariant,
        episodeMapping: EpisodeMapping,
        identifiers: HashSet<String>,
        episodes: MutableList<Episode>,
        releaseDateTime: ZonedDateTime,
    ) {
        runCatching {
            val id = StringUtils.getVideoOldIdOrId(episodeVariant.identifier!!) ?: return
            val ids = animePlatformService.findAllIdByAnimeAndPlatform(episodeMapping.anime!!, episodeVariant.platform!!)
            val platformEpisodes = ids.flatMap { PrimeVideoCachedWrapper.getEpisodesByShowId(countryCode, it).toList() }
            val episode = platformEpisodes.find { it.id == id || id in it.oldIds || (it.season == episodeMapping.season && it.number == episodeMapping.number) } ?: return

            updateIdentifier(episodeVariant, id, episode.id, identifiers)
            updateUrl(episodeVariant, episode.url)

            episodes.addAll(
                primeVideoPlatform.convertEpisode(
                    countryCode,
                    StringUtils.EMPTY_STRING,
                    episode,
                    releaseDateTime
                )
            )
        }
    }

    private suspend fun getCrunchyrollEpisodeAndVariants(
        countryCode: CountryCode,
        crunchyrollId: String,
        isImageUpdate: Boolean
    ): List<Episode> {
        val browseObjects = mutableListOf<BrowseObject>()

        val episodeSource = if (isImageUpdate) {
            CrunchyrollWrapper.getEpisode(countryCode.locale, crunchyrollId)
        } else {
            CrunchyrollCachedWrapper.getEpisode(countryCode.locale, crunchyrollId)
        }
        
        val mainObject = episodeSource.also { browseObjects.add(it.convertToBrowseObject()) }
        
        val variantIds = mainObject.getVariants().subtract(browseObjects.map { it.id }.toSet())
        val variantObjects = variantIds.chunked(AbstractCrunchyrollWrapper.CRUNCHYROLL_CHUNK)
            .flatMap { chunk -> CrunchyrollCachedWrapper.getObjects(countryCode.locale, *chunk.toTypedArray()) }

        return (browseObjects + variantObjects).mapNotNull { browseObject ->
            try {
                crunchyrollPlatform.convertEpisode(
                    countryCode,
                    browseObject,
                    needSimulcast = false,
                )
            } catch (e: Exception) {
                logger.warning("Error while getting Crunchyroll episode ${browseObject.id} : ${e.message}")
                null
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
                    countryCode,
                    AnimationDigitalNetworkCachedWrapper.getVideo(countryCode, id.toInt()).show.id,
                    id.toInt()
                )?.id?.toString()
            }.onFailure {
                logger.warning("Error while getting previous episode for $id: ${it.message}")
            }.getOrNull()

            Platform.CRUN -> {
                // Attempt to fetch the previous episode directly
                runCatching { CrunchyrollCachedWrapper.getPreviousEpisode(countryCode.locale, id) }.getOrNull()?.let { return it.id }

                logger.warning("Cannot fetch previous episode for $id, trying alternative methods...")

                val episode = runCatching { CrunchyrollCachedWrapper.getEpisode(countryCode.locale, id) }.getOrNull() ?: return null

                // Fetch episodes by season and find the previous episode
                runCatching { CrunchyrollCachedWrapper.getEpisodesBySeasonId(countryCode.locale, episode.seasonId) }.getOrNull()
                    ?.sortedBy { it.sequenceNumber }
                    ?.firstOrNull { it.sequenceNumber < episode.sequenceNumber }
                    ?.let { return it.id }

                // Fetch episodes by series and find the previous episode
                logger.warning("Previous episode not found in season, searching by series...")
                return runCatching { CrunchyrollCachedWrapper.getEpisodesBySeriesId(countryCode.locale, episode.seriesId) }.getOrNull()
                    ?.sortedWith(compareBy({ it.episodeMetadata!!.seasonSequenceNumber }, { it.episodeMetadata!!.sequenceNumber }))
                    ?.lastOrNull { it.episodeMetadata!!.index() < episode.index() }
                    ?.id
            }

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
                    countryCode,
                    AnimationDigitalNetworkCachedWrapper.getVideo(countryCode, id.toInt()).show.id,
                    id.toInt()
                )?.id?.toString()
            }.onFailure {
                logger.warning("Error while getting next episode for $id: ${it.message}")
            }.getOrNull()

            Platform.CRUN -> {
                // Attempt to fetch the next episode directly
                runCatching { CrunchyrollCachedWrapper.getUpNext(countryCode.locale, id) }.getOrNull()?.let { return it.id }

                logger.warning("Cannot fetch next episode for $id, trying alternative methods...")

                // Fetch the current episode and check for nextEpisodeId
                val episode = runCatching { CrunchyrollCachedWrapper.getEpisode(countryCode.locale, id) }.getOrNull() ?: return null

                episode.nextEpisodeId?.let { return it }

                // Fetch episodes by season and find the next episode
                logger.warning("Next episode ID not found for $id, searching by season...")
                runCatching { CrunchyrollCachedWrapper.getEpisodesBySeasonId(countryCode.locale, episode.seasonId) }.getOrNull()
                    ?.sortedBy { it.sequenceNumber }
                    ?.firstOrNull { it.sequenceNumber > episode.sequenceNumber }
                    ?.let { return it.id }

                // Fetch episodes by series and find the next episode
                logger.warning("Next episode not found in season, searching by series...")
                return runCatching { CrunchyrollCachedWrapper.getEpisodesBySeriesId(countryCode.locale, episode.seriesId) }.getOrNull()
                    ?.sortedWith(compareBy({ it.episodeMetadata!!.seasonSequenceNumber }, { it.episodeMetadata!!.sequenceNumber }))
                    ?.firstOrNull { it.episodeMetadata!!.index() > episode.index() }
                    ?.id
            }

            else -> {
                logger.warning("Error while getting next episode $id: Invalid platform")
                null
            }
        }
    }
}