package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.*
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.*
import fr.shikkanime.wrappers.impl.*
import fr.shikkanime.wrappers.impl.caches.*
import io.ktor.client.plugins.*
import java.time.ZonedDateTime

class OptimizeUpdateEpisodeJob : AbstractJob {
    data class Context(
        val zonedDateTime: ZonedDateTime,
        val countryCode: CountryCode,
        val anime: Anime,
        val episodeMapping: EpisodeMapping,
        val episodeVariant: EpisodeVariant,
        val episodePlatformId: String,
        val variantIdentifierInTimeout: MutableList<String>,
        val platformEpisodes: MutableList<AbstractPlatform.Episode>,
        val shouldIgnoreCache: Boolean,
        val previousAndNextEpisodes: MutableList<AbstractPlatform.Episode> = mutableListOf()
    )

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject
    private lateinit var mailService: MailService

    @Inject private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform
    @Inject private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @Inject private lateinit var disneyPlusPlatform: DisneyPlusPlatform
    @Inject private lateinit var netflixPlatform: NetflixPlatform
    @Inject
    private lateinit var primeVideoPlatform: PrimeVideoPlatform

    private fun <T> updateIfChanged(
        identifier: String,
        fieldName: String,
        candidate: T?,
        current: T?,
        isValid: (T?) -> Boolean,
        apply: (T) -> Unit
    ) {
        if (!(isValid(candidate) && candidate != current))
            return

        apply(candidate as T)
        logger.info("Updating $fieldName for $identifier to $candidate")
    }

    override suspend fun run() {
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val lastImageUpdateDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_EPISODE_DELAY, 2).toLong())
        val episodesNeedingUpdate = episodeMappingService.findAllNeedUpdate()
        logger.info("Found ${episodesNeedingUpdate.size} episodes to update")

        val needUpdateEpisodes = episodesNeedingUpdate
            .shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_SIZE, 15))

        if (needUpdateEpisodes.isEmpty()) {
            logger.info("No episode to update")
            return
        }

        var needInvalidation = false
        val notKnownEpisodes = mutableListOf<Pair<AbstractPlatform.Episode, EpisodeMapping?>>()

        // Can it be parallel?
        needUpdateEpisodes.forEach { episodeMapping ->
            val countryCode = episodeMapping.anime!!.countryCode!!
            val mappingIdentifier = "${StringUtils.getShortName(episodeMapping.anime!!.name!!)} ${StringUtils.toEpisodeMappingString(episodeMapping)}"
            val shouldIgnoreCache = attachmentService.findByEntityUuidTypeAndActive(episodeMapping.uuid!!, ImageType.BANNER)?.url == Constant.DEFAULT_IMAGE_PREVIEW && episodeMapping.releaseDateTime.isAfterOrEqual(lastImageUpdateDateTime)
            logger.info("Updating episode $mappingIdentifier... (Should ignore cache: $shouldIgnoreCache)")

            val episodeVariants = episodeVariantService.findAllByMapping(episodeMapping)
            val platformEpisodes = mutableListOf<AbstractPlatform.Episode>()
            val variantIdentifierInTimeout = mutableListOf<String>()

            episodeVariants.forEach { episodeVariant ->
                val episodePlatformId = StringUtils.getVideoOldIdOrId(episodeVariant.identifier!!)!!
                val context = Context(
                    zonedDateTime,
                    countryCode,
                    episodeMapping.anime!!,
                    episodeMapping,
                    episodeVariant,
                    episodePlatformId,
                    variantIdentifierInTimeout,
                    platformEpisodes,
                    shouldIgnoreCache
                )

                when (episodeVariant.platform!!) {
                    Platform.ANIM -> fetchADN(context)
                    Platform.CRUN -> fetchCrunchyroll(context)
                    Platform.DISN -> fetchDisneyPlus(context)
                    Platform.NETF -> fetchNetflix(context)
                    Platform.PRIM -> fetchPrimeVideo(context)
                    else -> logger.warning("Unknown platform ${episodeVariant.platform!!.platformName}")
                }

                if (variantIdentifierInTimeout.contains(episodeVariant.identifier)) {
                    logger.warning("${episodeVariant.identifier} failed to fetch on timeout, retry update later")
                    return@forEach
                }

                notKnownEpisodes.addAll(context.previousAndNextEpisodes.map { it to null })
                val predicate: (AbstractPlatform.Episode) -> Boolean = { it.getIdentifier() == episodeVariant.identifier }

                if (platformEpisodes.none(predicate)) {
                    if (episodeVariant.available) {
                        needInvalidation = true
                        logger.warning("${episodeVariant.identifier} not found for $mappingIdentifier, variant will be marked unavailable")
                        episodeVariant.available = false
                        episodeVariantService.update(episodeVariant)
                        traceActionService.createTraceAction(episodeVariant, TraceAction.Action.UPDATE)
                    }

                    return@forEach
                }

                // Old ids on AbstractEpisode?
                val matchingVariant = platformEpisodes.singleOrNull(predicate)
                var hasChanged = false

                updateIfChanged(
                    episodeVariant.identifier!!,
                    fieldName = "url",
                    candidate = matchingVariant?.url,
                    current = episodeVariant.url,
                    isValid = { !it.isNullOrBlank() },
                    apply = { episodeVariant.url = it; hasChanged = true; needInvalidation = true }
                )

                updateIfChanged(
                    episodeVariant.identifier!!,
                    fieldName = "available",
                    candidate = true,
                    current = episodeVariant.available,
                    isValid = { it != null },
                    apply = { episodeVariant.available = it; hasChanged = true; needInvalidation = true }
                )

                if (hasChanged) {
                    episodeVariantService.update(episodeVariant)
                    traceActionService.createTraceAction(episodeVariant, TraceAction.Action.UPDATE)
                }
            }

            if (variantIdentifierInTimeout.isNotEmpty()) {
                logger.warning("${variantIdentifierInTimeout.size} variant(s) failed to fetch on timeout, retry update later")
                return@forEach
            }

            val variantIdentifiers = episodeVariants.map(EpisodeVariant::identifier).toSet()
            val matchedAndKnownEpisodes = platformEpisodes.filter { it.getIdentifier() in variantIdentifiers }.sortedBy { it.platform.sortIndex }
            var hasChanged = false

            updateIfChanged(
                mappingIdentifier,
                fieldName = "image",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull(AbstractPlatform.Episode::image),
                current = attachmentService.findByEntityUuidTypeAndActive(episodeMapping.uuid, ImageType.BANNER)?.url,
                isValid = { it != null },
                apply = {
                    attachmentService.createAttachmentOrMarkAsActive(
                        episodeMapping.uuid,
                        ImageType.BANNER,
                        it
                    ); hasChanged = true; needInvalidation = true
                }
            )

            updateIfChanged(
                mappingIdentifier,
                fieldName = "title",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull { it.title.normalize() },
                current = episodeMapping.title.normalize(),
                isValid = { !it.isNullOrBlank() },
                apply = { episodeMapping.title = it; hasChanged = true; needInvalidation = true }
            )

            updateIfChanged(
                mappingIdentifier,
                fieldName = "description",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull { it.description.normalize() },
                current = episodeMapping.description.normalize(),
                isValid = { !it.isNullOrBlank() },
                apply = { episodeMapping.description = it; hasChanged = true; needInvalidation = true }
            )

            updateIfChanged(
                mappingIdentifier,
                fieldName = "duration",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull(AbstractPlatform.Episode::duration),
                current = episodeMapping.duration,
                isValid = { it != null && it > 0 },
                apply = { episodeMapping.duration = it; hasChanged = true; needInvalidation = true }
            )

            if (hasChanged) {
                episodeMapping.lastUpdateDateTime = zonedDateTime
                episodeMappingService.update(episodeMapping)
                traceActionService.createTraceAction(episodeMapping, TraceAction.Action.UPDATE)
            }

            notKnownEpisodes.addAll(platformEpisodes.filter { it.getIdentifier() !in variantIdentifiers }
                .map { it to episodeMapping })
            logger.info("Episode $mappingIdentifier updated")
        }

        val allIdentifiers = episodeVariantService.findAllIdentifiers()
        val notKnownAllEpisodes = notKnownEpisodes.filter { it.first.getIdentifier() !in allIdentifiers }
            .distinctBy { it.first.getIdentifier() }
        logger.info("Found ${notKnownAllEpisodes.size} not known episodes: ${notKnownAllEpisodes.joinToString { it.first.getIdentifier() }}")
        notKnownAllEpisodes.forEach { (episode, mapping) -> episodeVariantService.save(episode, false, mapping) }

        if (needInvalidation || notKnownAllEpisodes.isNotEmpty()) {
            logger.info("Invalidating cache...")
            animeService.recalculateSimulcasts()
            episodeVariantService.preIndex()
            InvalidationService.invalidate(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                Simulcast::class.java
            )

            if (notKnownAllEpisodes.isNotEmpty()) {
                mailService.saveAdminMail(
                    title = "UpdateEpisodeMappingJob - ${notKnownAllEpisodes.size} new episodes",
                    body = notKnownAllEpisodes.joinToString("<br>") { "<b>${it.first.anime}</b>: S${it.first.season} ${it.first.episodeType} ${it.first.number} (<b>${it.first.getIdentifier()}</b>)" }
                )
            }
        }

        logger.info("All episodes processed")
    }

    private suspend fun <T> fetchOrSkip(
        variantIdentifierInTimeout: MutableList<String>,
        episodeVariant: EpisodeVariant,
        fetch: suspend () -> T
    ): T? = try {
        HttpRequest.retryOnTimeout(3) { fetch() }
    } catch (_: HttpRequestTimeoutException) {
        variantIdentifierInTimeout.add(episodeVariant.identifier!!)
        null
    } catch (e: Exception) {
        logger.warning("Error while fetching ${episodeVariant.platform!!.platformName} content for ${episodeVariant.identifier!!}: ${e.message}")
        null
    }

    private suspend fun fetchNeighbors(
        context: Context,
        getPrevious: suspend (String) -> Any?,
        getNext: suspend (String) -> Any?,
        convert: suspend (Any) -> List<AbstractPlatform.Episode>
    ) {
        val checkPreviousAndNextEpisodes = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_PREVIOUS_AND_NEXT_EPISODES)
        if (!checkPreviousAndNextEpisodes) return

        val depth = configCacheService.getValueAsInt(ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH, 1)
        var previousId: String? = context.episodePlatformId
        var nextId: String? = context.episodePlatformId

        repeat(depth) {
            val prev = previousId?.let { id -> runCatching { getPrevious(id) }.getOrNull() }
            val next = nextId?.let { id -> runCatching { getNext(id) }.getOrNull() }

            prev?.let {
                val converted = convert(it)
                context.previousAndNextEpisodes.addAll(converted)
                previousId = converted.firstOrNull()?.id
            } ?: run { previousId = null }

            next?.let {
                val converted = convert(it)
                context.previousAndNextEpisodes.addAll(converted)
                nextId = converted.firstOrNull()?.id
            } ?: run { nextId = null }
        }
    }

    private suspend fun fetchADN(context: Context) {
        val video = fetchOrSkip(context.variantIdentifierInTimeout, context.episodeVariant) {
            if (context.shouldIgnoreCache)
                AnimationDigitalNetworkWrapper.getVideo(context.countryCode, context.episodePlatformId.toInt())
            else
                AnimationDigitalNetworkCachedWrapper.getVideo(context.countryCode, context.episodePlatformId.toInt())
        } ?: return

        context.platformEpisodes.addAll(
            runCatching {
                animationDigitalNetworkPlatform.convertEpisode(
                    context.countryCode,
                    video,
                    context.zonedDateTime,
                    needSimulcast = false,
                    checkAnimation = false
                )
            }.getOrDefault(emptyList())
        )

        fetchNeighbors(
            context,
            getPrevious = { AnimationDigitalNetworkCachedWrapper.getPreviousVideo(context.countryCode, video.show.id, it.toInt()) },
            getNext = { AnimationDigitalNetworkCachedWrapper.getNextVideo(context.countryCode, video.show.id, it.toInt()) },
            convert = { runCatching {
                animationDigitalNetworkPlatform.convertEpisode(
                    context.countryCode,
                    it as AbstractAnimationDigitalNetworkWrapper.Video,
                    context.zonedDateTime,
                    needSimulcast = false,
                    checkAnimation = false
                )
            }.getOrDefault(emptyList()) }
        )
    }

    private suspend fun fetchCrunchyroll(context: Context) {
        val episode = fetchOrSkip(context.variantIdentifierInTimeout, context.episodeVariant) {
            if (context.shouldIgnoreCache)
                CrunchyrollWrapper.getObjects(context.countryCode.locale, context.episodePlatformId)
            else
                CrunchyrollCachedWrapper.getObjects(context.countryCode.locale, context.episodePlatformId)
        }?.firstOrNull() ?: return

        val allAudioLocales = (episode.episodeMetadata!!.versions?.map { it.audioLocale }?.toSet() ?: emptySet()) + setOf(episode.episodeMetadata.audioLocale)
        val allowedAudioLocales = LocaleUtils.getAllowedLocales(context.countryCode, allAudioLocales)
        val variantObjects = mutableListOf(episode)

        episode.episodeMetadata.versions
            ?.filter { it.guid != episode.id && it.audioLocale in allowedAudioLocales }
            ?.distinct()
            ?.chunked(AbstractCrunchyrollWrapper.CRUNCHYROLL_CHUNK)
            ?.flatMap { chunk ->
                CrunchyrollCachedWrapper.getObjects(
                    context.countryCode.locale,
                    *chunk.map(AbstractCrunchyrollWrapper.Version::guid).toTypedArray()
                )
            }?.let(variantObjects::addAll)

        context.platformEpisodes.addAll(
            variantObjects.mapNotNull {
                runCatching {
                    crunchyrollPlatform.convertEpisode(
                        context.countryCode,
                        it,
                        needSimulcast = false
                    )
                }.getOrNull()
            }
        )

        fetchNeighbors(
            context,
            getPrevious = { CrunchyrollCachedWrapper.retrievePreviousEpisode(context.countryCode.locale, it) },
            getNext = { CrunchyrollCachedWrapper.retrieveNextEpisode(context.countryCode.locale, it) },
            convert = {
                listOfNotNull(
                    runCatching {
                        crunchyrollPlatform.convertEpisode(
                            context.countryCode,
                            it as AbstractCrunchyrollWrapper.BrowseObject,
                            needSimulcast = false
                        )
                    }.getOrNull()
                )
            }
        )
    }

    private suspend fun fetchDisneyPlus(context: Context) {
        val animePlatformIds = animePlatformService.findAllIdByAnimeAndPlatform(context.anime, context.episodeVariant.platform!!)
        val shouldCheckAudioLocales = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)

        val episodes = animePlatformIds.flatMap { animePlatformId ->
            fetchOrSkip(context.variantIdentifierInTimeout, context.episodeVariant) {
                if (context.shouldIgnoreCache)
                    DisneyPlusWrapper.getEpisodesByShowId(context.countryCode, animePlatformId, shouldCheckAudioLocales)
                else
                    DisneyPlusCachedWrapper.getEpisodesByShowId(context.countryCode, animePlatformId, shouldCheckAudioLocales)
            }?.toList() ?: emptyList()
        }

        val episode = episodes.find { it.id == context.episodePlatformId || it.oldId == context.episodePlatformId } ?: return

        context.platformEpisodes.addAll(
            disneyPlusPlatform.convertEpisode(
                context.countryCode,
                episode,
                context.zonedDateTime
            )
        )

        fetchNeighbors(
            context,
            getPrevious = { DisneyPlusCachedWrapper.getPreviousEpisode(context.countryCode, episode.show.id, it, shouldCheckAudioLocales) },
            getNext = { DisneyPlusCachedWrapper.getNextEpisode(context.countryCode, episode.show.id, it, shouldCheckAudioLocales) },
            convert = {
                disneyPlusPlatform.convertEpisode(
                    context.countryCode,
                    it as AbstractDisneyPlusWrapper.Episode,
                    context.zonedDateTime
                )
            }
        )
    }

    private suspend fun fetchNetflix(context: Context) {
        val animePlatformIds = animePlatformService.findAllIdByAnimeAndPlatform(context.anime, context.episodeVariant.platform!!)

        val episodes = animePlatformIds.flatMap { animePlatformId ->
            fetchOrSkip(context.variantIdentifierInTimeout, context.episodeVariant) {
                if (context.shouldIgnoreCache)
                    NetflixWrapper.getEpisodesByShowId(context.countryCode, animePlatformId.toInt())
                else
                    NetflixCachedWrapper.getEpisodesByShowId(context.countryCode, animePlatformId.toInt())
            }?.toList() ?: emptyList()
        }

        val episode = episodes.find { it.id.toString() == context.episodePlatformId || it.oldId == context.episodePlatformId } ?: return

        context.platformEpisodes.addAll(
            netflixPlatform.convertEpisode(
                context.countryCode,
                episode
            )
        )

        fetchNeighbors(
            context,
            getPrevious = { NetflixCachedWrapper.getPreviousEpisode(context.countryCode, episode.show.id, it.toInt()) },
            getNext = { NetflixCachedWrapper.getNextEpisode(context.countryCode, episode.show.id, it.toInt()) },
            convert = {
                netflixPlatform.convertEpisode(
                    context.countryCode,
                    it as AbstractNetflixWrapper.Episode,
                )
            }
        )
    }

    private suspend fun fetchPrimeVideo(context: Context) {
        val animePlatformIds =
            animePlatformService.findAllIdByAnimeAndPlatform(context.anime, context.episodeVariant.platform!!)

        val episodes = animePlatformIds.flatMap { animePlatformId ->
            fetchOrSkip(context.variantIdentifierInTimeout, context.episodeVariant) {
                if (context.shouldIgnoreCache)
                    PrimeVideoWrapper.getEpisodesByShowId(context.countryCode, animePlatformId)
                else
                    PrimeVideoCachedWrapper.getEpisodesByShowId(context.countryCode, animePlatformId)
            }?.toList() ?: emptyList()
        }

        val episode =
            episodes.find { it.id == context.episodePlatformId || context.episodePlatformId in it.oldIds } ?: return

        context.platformEpisodes.addAll(
            primeVideoPlatform.convertEpisode(
                context.countryCode,
                StringUtils.EMPTY_STRING,
                episode,
                context.zonedDateTime
            )
        )

        fetchNeighbors(
            context,
            getPrevious = { PrimeVideoCachedWrapper.getPreviousEpisode(context.countryCode, episode.show.id, it) },
            getNext = { PrimeVideoCachedWrapper.getNextEpisode(context.countryCode, episode.show.id, it) },
            convert = {
                primeVideoPlatform.convertEpisode(
                    context.countryCode,
                    StringUtils.EMPTY_STRING,
                    it as AbstractPrimeVideoWrapper.Episode,
                    context.zonedDateTime
                )
            }
        )
    }
}