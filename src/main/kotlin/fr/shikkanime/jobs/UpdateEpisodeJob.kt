package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.ZonedDateTime

class UpdateEpisodeJob : AbstractJob {
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

    private val adnCache = MapCache<CountryCodeIdKeyCache, List<Episode>>(duration = Duration.ofDays(1)) {
        return@MapCache runBlocking {
            val video = try {
                AnimationDigitalNetworkWrapper.getShowVideo(it.id)
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
        // Take 15 episodes of a platform, and if the lastUpdate is older than 30 days, or if the episode mapping is valid
        val lastDateTime = ZonedDateTime.now().minusDays(30)
        val adnEpisodes = episodeMappingService.findAllNeedUpdateByPlatform(Platform.ANIM, lastDateTime)
        val crunchyrollEpisodes = episodeMappingService.findAllNeedUpdateByPlatform(Platform.CRUN, lastDateTime)

        logger.info("Found ${adnEpisodes.size} ADN episodes and ${crunchyrollEpisodes.size} Crunchyroll episodes to update")

        val needUpdateEpisodes = (adnEpisodes + crunchyrollEpisodes).distinctBy { it.uuid }
            .shuffled()
            .take(15)

        if (needUpdateEpisodes.isEmpty()) {
            logger.info("No episode to update")
            return
        }

        var needRecalculate = false
        var needRefreshCache = false
        val identifiers = episodeVariantService.findAllIdentifiers()

        needUpdateEpisodes.forEach { mapping ->
            val variants = episodeVariantService.findAllByMapping(mapping)
            val mappingIdentifier = "${StringUtils.getShortName(mapping.anime!!.name!!)} - S${mapping.season} ${mapping.episodeType} ${mapping.number}"
            logger.info("Updating episode $mappingIdentifier...")
            val episodes =
                variants.flatMap { variant -> runBlocking { retrievePlatformEpisode(mapping, variant) } }

            episodes.filter { it.getIdentifier() !in identifiers }.takeIf { it.isNotEmpty() }
                ?.also { logger.info("Found ${it.size} new episodes for $mappingIdentifier") }
                ?.map { episode -> episodeVariantService.save(episode, false, mapping) }
                ?.also {
                    logger.info("Added ${it.size} episodes for $mappingIdentifier")
                    needRecalculate = true
                    needRefreshCache = true
                }

            val originalEpisode = episodes.firstOrNull { it.original }
                ?: episodes.firstOrNull()
                ?: return@forEach run {
                    logger.warning("No episode found for $mappingIdentifier")
                    mapping.lastUpdateDateTime = ZonedDateTime.now()
                    episodeMappingService.update(mapping)
                    logger.info("Episode $mappingIdentifier updated")
                }

            var hasChanged = false

            if (originalEpisode.image != Constant.DEFAULT_IMAGE_PREVIEW && mapping.image != originalEpisode.image) {
                mapping.image = originalEpisode.image
                episodeMappingService.addImage(mapping.uuid!!, originalEpisode.image, true)
                logger.info("Image updated for $mappingIdentifier to ${originalEpisode.image}")
                hasChanged = true
                needRefreshCache = true
            }

            if (originalEpisode.title != mapping.title && !originalEpisode.title.isNullOrBlank()) {
                mapping.title = originalEpisode.title
                logger.info("Title updated for $mappingIdentifier to ${originalEpisode.title}")
                hasChanged = true
                needRefreshCache = true
            }

            val trimmedDescription = originalEpisode.description?.take(Constant.MAX_DESCRIPTION_LENGTH)

            if (trimmedDescription != mapping.description &&
                !trimmedDescription.isNullOrBlank() &&
                languageCacheService.detectLanguage(trimmedDescription) == mapping.anime!!.countryCode!!.name.lowercase()
            ) {
                mapping.description = trimmedDescription
                logger.info("Description updated for $mappingIdentifier to $trimmedDescription")
                hasChanged = true
                needRefreshCache = true
            }

            mapping.status = StringUtils.getStatus(mapping)
            mapping.lastUpdateDateTime = ZonedDateTime.now()
            episodeMappingService.update(mapping)

            if (hasChanged) {
                traceActionService.createTraceAction(mapping, TraceAction.Action.UPDATE)
            }

            logger.info("Episode $mappingIdentifier updated")
        }

        if (needRecalculate) {
            logger.info("Recalculating simulcasts...")
            animeService.recalculateSimulcasts()
        }

        logger.info("Episodes updated")

        if (needRefreshCache) {
            MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
        }
    }

    private suspend fun retrievePlatformEpisode(
        episodeMapping: EpisodeMapping,
        episodeVariant: EpisodeVariant
    ): List<Episode> {
        val countryCode = episodeMapping.anime!!.countryCode!!
        val episodes = mutableListOf<Episode>()

        if (episodeVariant.platform == Platform.ANIM) {
            val adnId = "[A-Z]{2}-ANIM-([0-9]{1,5})-[A-Z]{2}-[A-Z]{2}(?:-UNC)?".toRegex()
                .find(episodeVariant.identifier!!)?.groupValues?.get(1)

            if (adnId.isNullOrBlank()) {
                logger.warning("Error while getting ADN episode $adnId : Invalid ADN ID")
                return emptyList()
            }

            episodes.addAll(adnCache[CountryCodeIdKeyCache(countryCode, adnId)]!!)
        }

        if (episodeVariant.platform == Platform.CRUN) {
            episodes.addAll(
                getCrunchyrollEpisodeAndVariants(
                    countryCode,
                    crunchyrollPlatform.getCrunchyrollId(episodeVariant.identifier!!)!!
                )
            )
        }

        return episodes
    }

    private suspend fun getCrunchyrollEpisodeAndVariants(
        countryCode: CountryCode,
        crunchyrollId: String,
    ): List<Episode> {
        val crunchyrollEpisode = CrunchyrollWrapper.getObjects(countryCode.locale, crunchyrollPlatform.identifiers[countryCode]!!, crunchyrollId).first()

        val versionIds = crunchyrollEpisode.episodeMetadata!!.versions?.toMutableList() ?: mutableListOf(CrunchyrollWrapper.Version(crunchyrollEpisode.id, true))
        versionIds.removeIf { it.guid.isBlank() || it.guid == crunchyrollId }

        val crunchyrollEpisodes = if (versionIds.isNotEmpty()) {
            versionIds.asSequence()
                .distinctBy { variant -> variant.guid }
                .chunked(50)
                .flatMap { chunk ->
                    try {
                        runBlocking {
                            CrunchyrollWrapper.getObjects(
                                countryCode.locale,
                                crunchyrollPlatform.identifiers[countryCode]!!,
                                *chunk.map { it.guid }.toTypedArray()
                            ).toList()
                        }
                    } catch (e: Exception) {
                        logger.warning("Error while fetching Crunchyroll chunked variants: ${e.message}")
                        emptyList()
                    }
                }
                .toList()
        } else {
            listOf(crunchyrollEpisode)
        }

        return crunchyrollEpisodes.mapNotNull { browseObject ->
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
}