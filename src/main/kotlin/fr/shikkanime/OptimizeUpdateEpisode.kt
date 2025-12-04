package fr.shikkanime

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.jobs.AbstractJob
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.platforms.DisneyPlusPlatform
import fr.shikkanime.platforms.NetflixPlatform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import fr.shikkanime.wrappers.impl.caches.AnimationDigitalNetworkCachedWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import fr.shikkanime.wrappers.impl.caches.DisneyPlusCachedWrapper
import fr.shikkanime.wrappers.impl.caches.NetflixCachedWrapper
import io.ktor.client.plugins.*
import java.time.ZonedDateTime
import kotlin.system.exitProcess

suspend fun main() {
    val optimizeUpdateEpisodeJob = Constant.injector.getInstance(OptimizeUpdateEpisodeJob::class.java)
    optimizeUpdateEpisodeJob.run()
    exitProcess(0)
}

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
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animePlatformService: AnimePlatformService

    @Inject private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform
    @Inject private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @Inject private lateinit var disneyPlusPlatform: DisneyPlusPlatform
    @Inject private lateinit var netflixPlatform: NetflixPlatform

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
        val lastUpdateDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY, 30).toLong())
        val lastImageUpdateDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_EPISODE_DELAY, 2).toLong())
        val episodesNeedingUpdate = episodeMappingService.findAllNeedUpdate(lastUpdateDateTime, lastImageUpdateDateTime)
        logger.info("Found ${episodesNeedingUpdate.size} episodes to update")

        val needUpdateEpisodes = episodesNeedingUpdate
            .shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_SIZE, 15))

        if (needUpdateEpisodes.isEmpty()) {
            logger.info("No episode to update")
            return
        }

        val allIdentifiers = episodeVariantService.findAllIdentifiers()

        // Can it be parallel?
        needUpdateEpisodes.forEach { episodeMapping ->
            val countryCode = episodeMapping.anime!!.countryCode!!
            val mappingIdentifier = "${StringUtils.getShortName(episodeMapping.anime!!.name!!)} ${StringUtils.toEpisodeMappingString(episodeMapping)}"
            val shouldIgnoreCache = attachmentService.findByEntityUuidTypeAndActive(episodeMapping.uuid!!, ImageType.BANNER)?.url == Constant.DEFAULT_IMAGE_PREVIEW && episodeMapping.releaseDateTime.isAfterOrEqual(lastImageUpdateDateTime)
            logger.info("Updating episode $mappingIdentifier... (Should ignore cache: $shouldIgnoreCache)")

            val episodeVariants = episodeVariantService.findAllByMapping(episodeMapping)
            val platformEpisodes = mutableListOf<AbstractPlatform.Episode>()
            val variantIdentifierInTimeout = mutableListOf<String>()
            val previousAndNextEpisodes = mutableListOf<AbstractPlatform.Episode>()

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
                    else -> logger.warning("Unknown platform ${episodeVariant.platform!!.platformName}")
                }

                if (platformEpisodes.none { it.getIdentifier() == episodeVariant.identifier } && !variantIdentifierInTimeout.contains(episodeVariant.identifier)) {
                    logger.warning("${episodeVariant.identifier} not found for $mappingIdentifier, variant will be marked unavailable")
                    // TODO: Make episode variant unavailable
                    return@forEach
                }

                previousAndNextEpisodes.addAll(context.previousAndNextEpisodes)

                // Old ids on AbstractEpisode?
                val matchingVariant = platformEpisodes.singleOrNull { it.id == episodePlatformId || it.getIdentifier() == episodeVariant.identifier }
                var hasChanged = false

                updateIfChanged(
                    episodeVariant.identifier!!,
                    fieldName = "identifier",
                    candidate = matchingVariant?.getIdentifier(),
                    current = episodeVariant.identifier,
                    isValid = { !it.isNullOrBlank() },
                    apply = { episodeVariant.identifier = it; hasChanged = true }
                )

                updateIfChanged(
                    episodeVariant.identifier!!,
                    fieldName = "url",
                    candidate = matchingVariant?.url,
                    current = episodeVariant.url,
                    isValid = { !it.isNullOrBlank() },
                    apply = { episodeVariant.url = it; hasChanged = true }
                )

                updateIfChanged(
                    episodeVariant.identifier!!,
                    fieldName = "uncensored",
                    candidate = matchingVariant?.uncensored,
                    current = episodeVariant.uncensored,
                    isValid = { it != null },
                    apply = { episodeVariant.uncensored = it; hasChanged = true }
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

            val variantIdentifiers = episodeVariants.map { it.identifier }.toSet()
            val notKnownEpisodes = platformEpisodes.filter { it.getIdentifier() !in variantIdentifiers && it.getIdentifier() !in allIdentifiers }
            logger.config("Found ${notKnownEpisodes.size} not known episodes: ${notKnownEpisodes.joinToString { it.getIdentifier() }}")

            val notKnownPreviousAndNextEpisodes = previousAndNextEpisodes.distinctBy { it.getIdentifier() }
                .filter { it.getIdentifier() !in allIdentifiers }
            logger.config("Found ${notKnownPreviousAndNextEpisodes.size} not known previous and next episodes: ${notKnownPreviousAndNextEpisodes.joinToString { it.getIdentifier() }}")

            val matchedAndKnownEpisodes = platformEpisodes.filter { it.getIdentifier() in variantIdentifiers }.sortedBy { it.platform.sortIndex }
            var hasChanged = false

            updateIfChanged(
                mappingIdentifier,
                fieldName = "image",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull { it.image },
                current = attachmentService.findByEntityUuidTypeAndActive(episodeMapping.uuid, ImageType.BANNER)?.url,
                isValid = { it != null },
                apply = { attachmentService.createAttachmentOrMarkAsActive(episodeMapping.uuid, ImageType.BANNER, it); hasChanged = true }
            )

            updateIfChanged(
                mappingIdentifier,
                fieldName = "title",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull { it.title.normalize() },
                current = episodeMapping.title.normalize(),
                isValid = { !it.isNullOrBlank() },
                apply = { episodeMapping.title = it; hasChanged = true }
            )

            updateIfChanged(
                mappingIdentifier,
                fieldName = "description",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull { it.description.normalize() },
                current = episodeMapping.description.normalize(),
                isValid = { !it.isNullOrBlank() },
                apply = { episodeMapping.description = it; hasChanged = true }
            )

            updateIfChanged(
                mappingIdentifier,
                fieldName = "duration",
                candidate = matchedAndKnownEpisodes.firstNotNullOfOrNull { it.duration },
                current = episodeMapping.duration,
                isValid = { it != null && it > 0 },
                apply = { episodeMapping.duration = it; hasChanged = true }
            )

            if (hasChanged) {
//                episodeMapping.lastUpdateDateTime = zonedDateTime
//                episodeMappingService.update(episodeMapping)
//                traceActionService.createTraceAction(episodeMapping, TraceAction.Action.UPDATE)
            }

            logger.info("Episode $mappingIdentifier updated")
        }
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

        val checkPreviousAndNextEpisodes = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_PREVIOUS_AND_NEXT_EPISODES)

        if (!checkPreviousAndNextEpisodes)
            return

        val depth = configCacheService.getValueAsInt(ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH, 1)
        var previousEpisodePlatformId: String? = context.episodePlatformId
        var nextEpisodePlatformId: String? = context.episodePlatformId

        repeat(depth) {
            val previousVideo = previousEpisodePlatformId?.let {
                AnimationDigitalNetworkCachedWrapper.getPreviousVideo(
                    context.countryCode,
                    video.show.id,
                    it.toInt()
                )
            }
            val nextVideo = nextEpisodePlatformId?.let {
                AnimationDigitalNetworkCachedWrapper.getNextVideo(
                    context.countryCode,
                    video.show.id,
                    it.toInt()
                )
            }

            previousEpisodePlatformId = previousVideo?.id?.toString()
            nextEpisodePlatformId = nextVideo?.id?.toString()

            previousVideo?.let {
                context.previousAndNextEpisodes.addAll(
                    runCatching {
                        animationDigitalNetworkPlatform.convertEpisode(
                            context.countryCode,
                            it,
                            context.zonedDateTime,
                            needSimulcast = false,
                            checkAnimation = false
                        )
                    }.getOrDefault(emptyList())
                )
            }

            nextVideo?.let {
                context.previousAndNextEpisodes.addAll(
                    runCatching {
                        animationDigitalNetworkPlatform.convertEpisode(
                            context.countryCode,
                            it,
                            context.zonedDateTime,
                            needSimulcast = false,
                            checkAnimation = false
                        )
                    }.getOrDefault(emptyList())
                )
            }
        }
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
                    *chunk.map { it.guid }.toTypedArray()
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

        val checkPreviousAndNextEpisodes = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_PREVIOUS_AND_NEXT_EPISODES)

        if (!checkPreviousAndNextEpisodes)
            return

        val depth = configCacheService.getValueAsInt(ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH, 1)
        var previousEpisodePlatformId: String? = context.episodePlatformId
        var nextEpisodePlatformId: String? = context.episodePlatformId

        repeat(depth) {
            val previousEpisode = previousEpisodePlatformId?.let {
                CrunchyrollCachedWrapper.retrievePreviousEpisode(context.countryCode.locale, it)
            }
            val nextEpisode = nextEpisodePlatformId?.let {
                CrunchyrollCachedWrapper.retrieveNextEpisode(context.countryCode.locale, it)
            }

            previousEpisodePlatformId = previousEpisode?.id
            nextEpisodePlatformId = nextEpisode?.id

            previousEpisode?.let {
                runCatching {
                    crunchyrollPlatform.convertEpisode(
                        context.countryCode,
                        it,
                        needSimulcast = false,
                    )
                }.onSuccess(context.previousAndNextEpisodes::add)
            }

            nextEpisode?.let {
                runCatching {
                    crunchyrollPlatform.convertEpisode(
                        context.countryCode,
                        it,
                    )
                }.onSuccess(context.previousAndNextEpisodes::add)
            }
        }
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
    }
}