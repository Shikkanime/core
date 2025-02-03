package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.impl.caches.AnimationDigitalNetworkCachedWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import fr.shikkanime.wrappers.impl.caches.NetflixCachedWrapper
import fr.shikkanime.wrappers.impl.caches.PrimeVideoCachedWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    data class UpdatableAnime(
        val platform: Platform,
        val lastReleaseDateTime: ZonedDateTime,
        val image: String,
        val banner: String,
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
    private lateinit var languageCacheService: LanguageCacheService

    override fun run() {
        val lastDateTime = ZonedDateTime.now()
            .minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_DELAY, 30).toLong())
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
                anime.status = StringUtils.getStatus(anime)
                anime.lastUpdateDateTime = ZonedDateTime.now()
                animeService.update(anime)
                return@forEach
            }

            var hasChanged = false
            val updatableImage = updatedAnimes.firstOrNull { it.image.isNotBlank() }?.image

            if (updatableImage != anime.image && !updatableImage.isNullOrBlank()) {
                anime.image = updatableImage
                animeService.addImage(anime.uuid!!, updatableImage, true)
                logger.info("Image updated for anime $shortName to $updatableImage")
                hasChanged = true
            }

            val updatableBanner = updatedAnimes.firstOrNull { it.banner.isNotBlank() }?.banner

            if (updatableBanner != anime.banner && !updatableBanner.isNullOrBlank()) {
                anime.banner = updatableBanner
                animeService.addBanner(anime.uuid!!, updatableBanner, true)
                logger.info("Banner updated for anime $shortName to $updatableBanner")
                hasChanged = true
            }

            val updatableDescription = updatedAnimes.firstOrNull { !it.description.isNullOrBlank() }?.description?.normalize()

            if (updatableDescription != anime.description && !updatableDescription.isNullOrBlank() && languageCacheService.detectLanguage(
                    updatableDescription
                ) == anime.countryCode!!.name.lowercase()
            ) {
                anime.description = updatableDescription
                logger.info("Description updated for anime $shortName to $updatableDescription")
                hasChanged = true
            }

            anime.status = StringUtils.getStatus(anime)
            anime.lastUpdateDateTime = ZonedDateTime.now()
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
                    Platform.NETF -> list.add(fetchNetflixAnime(it))
                    Platform.PRIM -> list.add(fetchPrimeVideoAnime(it))
                    else -> logger.warning("Platform ${it.platform} not supported")
                }
            }.onFailure { e ->
                logger.warning("Error while fetching anime ${anime.name} on platform ${it.platform}: ${e.message}")
            }
        }

        return list
    }

    private suspend fun fetchADNAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val show = AnimationDigitalNetworkCachedWrapper.getShow(animePlatform.platformId!!.toInt())
        val showVideos = AnimationDigitalNetworkCachedWrapper.getShowVideos(animePlatform.platformId!!.toInt())

        if (showVideos.isEmpty())
            throw Exception("No episode found for ADN anime ${show.title}")

        return UpdatableAnime(
            platform = Platform.ANIM,
            lastReleaseDateTime = showVideos.maxOf { it.releaseDate },
            image = show.fullHDImage,
            banner = show.fullHDBanner,
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
            image = series.fullHDImage!!,
            banner = series.fullHDBanner!!,
            description = series.description,
            episodeSize = objects.size
        )
    }

    private fun fetchNetflixAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val episodes = runCatching {
            NetflixCachedWrapper.getShowVideos(
                animePlatform.anime!!.countryCode!!,
                animePlatform.platformId!!
            )
        }.getOrNull()

        if (episodes.isNullOrEmpty())
            throw Exception("No episode found for Netflix anime ${animePlatform.anime!!.name}")

        val show = episodes.first().show

        return UpdatableAnime(
            platform = Platform.NETF,
            lastReleaseDateTime = animePlatform.anime!!.lastReleaseDateTime,
            image = animePlatform.anime!!.image!!,
            banner = show.banner,
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
            image = animePlatform.anime!!.image!!,
            banner = show.banner,
            description = show.description,
            episodeSize = episodes.size
        )
    }
}