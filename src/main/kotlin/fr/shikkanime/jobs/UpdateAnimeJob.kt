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
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    data class UpdatableAnime(
        val lastReleaseDateTime: ZonedDateTime,
        val image: String,
        val banner: String,
        val description: String?,
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
            logger.info("Updating anime ${StringUtils.getShortName(anime.name!!)}...")
            // Compare platform sort index and anime release date descending
            val updatedAnimes = runBlocking { fetchAnime(anime) }.sortedWith(
                compareBy<Pair<Platform, UpdatableAnime>> { it.first.sortIndex }
                    .thenByDescending { it.second.lastReleaseDateTime }
            ).map { it.second }

            if (updatedAnimes.isEmpty()) {
                logger.warning("No platform found for anime ${StringUtils.getShortName(anime.name!!)}")
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
                logger.info("Image updated for anime ${StringUtils.getShortName(anime.name!!)} to $updatableImage")
                hasChanged = true
            }

            val updatableBanner = updatedAnimes.firstOrNull { it.banner.isNotBlank() }?.banner

            if (updatableBanner != anime.banner && !updatableBanner.isNullOrBlank()) {
                anime.banner = updatableBanner
                animeService.addBanner(anime.uuid!!, updatableBanner, true)
                logger.info("Banner updated for anime ${StringUtils.getShortName(anime.name!!)} to $updatableBanner")
                hasChanged = true
            }

            val updatableDescription = updatedAnimes.firstOrNull { !it.description.isNullOrBlank() }?.description?.normalize()

            if (updatableDescription != anime.description && !updatableDescription.isNullOrBlank() && languageCacheService.detectLanguage(
                    updatableDescription
                ) == anime.countryCode!!.name.lowercase()
            ) {
                anime.description = updatableDescription
                logger.info("Description updated for anime ${StringUtils.getShortName(anime.name!!)} to $updatableDescription")
                hasChanged = true
            }

            anime.status = StringUtils.getStatus(anime)
            anime.lastUpdateDateTime = ZonedDateTime.now()
            animeService.update(anime)

            if (hasChanged) {
                traceActionService.createTraceAction(anime, TraceAction.Action.UPDATE)
            }

            logger.info("Anime ${StringUtils.getShortName(anime.name!!)} updated")
        }
    }

    private suspend fun fetchAnime(anime: Anime): List<Pair<Platform, UpdatableAnime>> {
        val list = mutableListOf<Pair<Platform, UpdatableAnime>>()

        animePlatformService.findAllByAnime(anime).forEach {
            when (it.platform!!) {
                Platform.ANIM -> list.add(it.platform to fetchADNAnime(it))
                Platform.CRUN -> list.add(it.platform to HttpRequest.retry(3) { fetchCrunchyrollAnime(it) })
                else -> logger.warning("Platform ${it.platform} not supported")
            }
        }

        return list
    }

    private suspend fun fetchADNAnime(animePlatform: AnimePlatform): UpdatableAnime {
        return AnimationDigitalNetworkWrapper.getShow(animePlatform.platformId!!)
            .let {
                UpdatableAnime(
                    lastReleaseDateTime = it.microdata!!.startDate,
                    image = it.image2x,
                    banner = it.imageHorizontal2x,
                    description = it.summary,
                )
            }
    }

    private suspend fun fetchCrunchyrollAnime(animePlatform: AnimePlatform): UpdatableAnime {
        val countryCode = animePlatform.anime!!.countryCode!!

        return CrunchyrollWrapper.getSeries(
            countryCode.locale,
            crunchyrollPlatform.identifiers[countryCode]!!,
            animePlatform.platformId!!
        ).let { series ->
            val objects = CrunchyrollWrapper.getEpisodesBySeriesId(
                countryCode.locale,
                crunchyrollPlatform.identifiers[countryCode]!!,
                series.id
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

            UpdatableAnime(
                lastReleaseDateTime = objects.maxOf { it.releaseDateTime },
                image = series.images.posterTall.first().maxBy { poster -> poster.width }.source,
                banner = series.images.posterWide.first().maxBy { poster -> poster.width }.source,
                description = series.description,
            )
        }
    }
}