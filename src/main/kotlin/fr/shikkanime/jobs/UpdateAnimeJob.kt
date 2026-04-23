package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.AniListMatchingService
import fr.shikkanime.services.AnimePlatformService
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.impl.caches.*
import io.ktor.client.plugins.*
import java.time.ZonedDateTime

class UpdateAnimeJob : AbstractJob {
    private data class AnimeData(
        val platform: Platform,
        val name: String,
        val attachments: Map<ImageType, String>,
        val description: String?,
    )

    private data class Context(
        val countryCode: CountryCode,
        val animePlatform: AnimePlatform,
        val animeInTimeout: MutableList<String>,
        val animeDatas: MutableList<AnimeData>
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var attachmentService: AttachmentService

    private fun <T> updateIfChanged(
        identifier: String,
        fieldName: String,
        candidate: T?,
        current: T,
        isValid: (T) -> Boolean,
        apply: (T) -> Unit
    ) {
        if (candidate == null || !(isValid(candidate) && candidate != current))
            return

        apply(candidate)
        logger.info("Updating $fieldName for $identifier to $candidate")
    }

    override suspend fun run() {
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val animesNeedingUpdate = animeService.findAllNeedUpdate()
        logger.info("Found ${animesNeedingUpdate.size} animes to update")

        val needUpdateAnimes =
            animesNeedingUpdate.take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_SIZE, 15))

        if (needUpdateAnimes.isEmpty()) {
            logger.info("No anime to update")
            return
        }

        val deprecatedAnimePlatformDateTime = zonedDateTime.minusMonths(
            configCacheService.getValueAsInt(
                ConfigPropertyKey.ANIME_PLATFORM_DEPRECATED_DURATION,
                3
            ).toLong()
        )
        var needInvalidation = false

        needUpdateAnimes.forEach { anime ->
            logger.info("Updating anime ${anime.name}...")

            val animePlatforms = animePlatformService.findAllByAnime(anime)
            val animeDatas = mutableListOf<AnimeData>()
            val animePlatformInTimeout = mutableListOf<String>()

            animePlatforms.filter { it.platform!!.isStreamingPlatform }
                .forEach { animePlatform ->
                    if (animePlatform.lastValidateDateTime != null && animePlatform.lastValidateDateTime!!.isBeforeOrEqual(
                            deprecatedAnimePlatformDateTime
                        )
                    ) {
                        logger.warning("Deleting old anime platform ${animePlatform.platform} for anime ${anime.name} with id ${animePlatform.platformId}")
                        animePlatformService.delete(animePlatform)
                        return@forEach
                    }

                    val context = Context(
                        anime.countryCode!!,
                        animePlatform,
                        animePlatformInTimeout,
                        animeDatas
                    )

                    when (animePlatform.platform!!) {
                        Platform.ANIM -> fetchADN(context)
                        Platform.CRUN -> fetchCrunchyroll(context)
                        Platform.DISN -> fetchDisneyPlus(context)
                        Platform.NETF -> fetchNetflix(context)
                        Platform.PRIM -> fetchPrimeVideo(context)
                        else -> logger.warning("Unknown platform ${animePlatform.platform.platformName}")
                    }

                    if (animePlatformInTimeout.contains(animePlatform.platformId)) {
                        logger.warning("${animePlatform.platformId} failed to fetch on timeout, retry update later")
                        return@forEach
                    }
                }

            if (animePlatformInTimeout.isNotEmpty()) {
                logger.warning("${animePlatformInTimeout.size} platform(s) failed to fetch on timeout, retry update later")
                return@forEach
            }

            // Check if a platform is present multiple times
            val platformCounts = animeDatas.groupingBy { it.platform }
                .eachCount()
                .filter { it.value > 1 }

            if (platformCounts.isNotEmpty()) {
                logger.warning("Found multiple platforms for anime ${anime.name}: ${platformCounts.keys.joinToString { it.platformName }}, using the one with same series name...")
                fun normalize(string: String) = string.replace(":", "")
                val animeNameNormalized = normalize(anime.name!!)

                platformCounts.forEach { (platform, _) ->
                    animeDatas.filter { it.platform == platform && normalize(it.name) != animeNameNormalized }
                        .forEach {
                            logger.warning("Removing ${it.name} from matching list because it doesn't match the anime name")
                            animeDatas.remove(it)
                        }
                }
            }

            require(animeDatas.isNotEmpty()) { "No data found for anime ${anime.name}" }
            val matchedAnimes = animeDatas.sortedBy { it.platform.sortIndex }

            ImageType.entries.forEach { imageType ->
                updateIfChanged(
                    anime.name!!,
                    fieldName = imageType.name.lowercase(),
                    candidate = matchedAnimes.firstNotNullOfOrNull { it.attachments[imageType] },
                    current = attachmentService.findByEntityUuidTypeAndActive(anime.uuid!!, imageType)?.url,
                    isValid = { !it.isNullOrBlank() },
                    apply = {
                        attachmentService.createAttachmentOrMarkAsActive(
                            anime.uuid,
                            imageType,
                            it
                        ); needInvalidation = true
                    }
                )
            }

            updateIfChanged(
                anime.name!!,
                fieldName = "description",
                candidate = matchedAnimes.firstNotNullOfOrNull {
                    it.description.normalize()?.take(Constant.MAX_DESCRIPTION_LENGTH)
                },
                current = anime.description,
                isValid = { !it.isNullOrBlank() },
                apply = { anime.description = it; needInvalidation = true }
            )

            animePlatforms.filterNot { it.platform!!.isStreamingPlatform }
                .sortedByDescending { it.lastValidateDateTime }
                .singleOrNull { it.platform == Platform.ANIL }
                ?.platformId?.toIntOrNull()
                ?.let { aniListMediaId -> runCatching { AniListCachedWrapper.getMediaById(aniListMediaId) } }
                ?.onSuccess { media ->
                    if (AniListMatchingService.updateAnimeGenreAndTags(
                            anime,
                            StringUtils.getShortName(anime.name!!),
                            media
                        )
                    ) {
                        logger.info("Genres or tags updated for anime ${anime.name}")
                    }
                }

            anime.lastUpdateDateTime = zonedDateTime
            animeService.update(anime)

            logger.info("Anime ${anime.name} updated")
        }

        if (needInvalidation)
            InvalidationService.invalidate(Anime::class.java)

        logger.info("All animes processed")
    }

    private suspend fun <T> fetchOrSkip(
        animeInTimeout: MutableList<String>,
        animePlatform: AnimePlatform,
        fetch: suspend () -> T
    ): T? = try {
        HttpRequest.retryOnTimeout(3) { fetch() }
    } catch (_: HttpRequestTimeoutException) {
        animeInTimeout.add(animePlatform.platformId!!)
        null
    } catch (e: Exception) {
        logger.warning("Error while fetching ${animePlatform.platform!!.platformName} content for ${animePlatform.platformId}: ${e.message}")
        null
    }

    private suspend fun fetchADN(context: Context) {
        val show = fetchOrSkip(context.animeInTimeout, context.animePlatform) {
            AnimationDigitalNetworkCachedWrapper.getShow(
                context.countryCode.name,
                context.animePlatform.platformId!!.toInt()
            )
        } ?: return

        context.animeDatas.add(
            AnimeData(
                platform = context.animePlatform.platform!!,
                name = show.shortTitle?.ifBlank { null } ?: show.title,
                attachments = mapOf(
                    ImageType.THUMBNAIL to show.fullHDImage,
                    ImageType.BANNER to show.fullHDBanner,
                    ImageType.CAROUSEL to show.fullHDCarousel,
                    ImageType.TITLE to show.fullHDTitle,
                ),
                description = show.summary
            )
        )
    }

    private suspend fun fetchCrunchyroll(context: Context) {
        val series = fetchOrSkip(context.animeInTimeout, context.animePlatform) {
            CrunchyrollCachedWrapper.getObjects(context.countryCode.locale, context.animePlatform.platformId!!).first()
        } ?: return
        val seasons = CrunchyrollCachedWrapper.getSeasonsBySeriesId(
            context.countryCode.locale,
            context.animePlatform.platformId!!
        )

        context.animeDatas.add(
            AnimeData(
                platform = context.animePlatform.platform!!,
                name = series.title!!,
                attachments = mapOf(
                    ImageType.THUMBNAIL to series.images!!.fullHDImage!!,
                    ImageType.BANNER to series.images.fullHDBanner!!,
                    ImageType.CAROUSEL to series.fullHDCarousel,
                    ImageType.TITLE to series.fullHDTitle,
                ),
                description = seasons.firstOrNull()?.description?.ifBlank { null } ?: series.getNormalizedDescription()
            )
        )
    }

    private suspend fun fetchDisneyPlus(context: Context) {
        val show = fetchOrSkip(context.animeInTimeout, context.animePlatform) {
            DisneyPlusCachedWrapper.getShow(context.animePlatform.platformId!!)
        } ?: return

        context.animeDatas.add(
            AnimeData(
                platform = context.animePlatform.platform!!,
                name = show.name,
                attachments = mapOf(
                    ImageType.THUMBNAIL to show.image,
                    ImageType.BANNER to show.banner,
                    ImageType.CAROUSEL to show.carousel,
                    ImageType.TITLE to show.title,
                ),
                description = show.description
            )
        )
    }

    private suspend fun fetchNetflix(context: Context) {
        val episodes = fetchOrSkip(context.animeInTimeout, context.animePlatform) {
            NetflixCachedWrapper.getEpisodesByShowId(context.countryCode, context.animePlatform.platformId!!.toInt())
        } ?: return

        if (episodes.isEmpty()) {
            logger.warning("No episode found for Netflix anime ${context.animePlatform.anime!!.name}")
            return
        }

        val show = episodes.first().show

        context.animeDatas.add(
            AnimeData(
                platform = context.animePlatform.platform!!,
                name = show.name,
                attachments = buildMap {
                    show.thumbnail?.let { put(ImageType.THUMBNAIL, it) }
                    put(ImageType.BANNER, show.banner)
                    put(ImageType.CAROUSEL, show.carousel)
                    show.title?.let { put(ImageType.TITLE, it) }
                },
                description = show.description
            )
        )
    }

    private suspend fun fetchPrimeVideo(context: Context) {
        val episodes = fetchOrSkip(context.animeInTimeout, context.animePlatform) {
            PrimeVideoCachedWrapper.getEpisodesByShowId(context.countryCode, context.animePlatform.platformId!!)
        } ?: return

        if (episodes.isEmpty()) {
            logger.warning("No episode found for PrimeVideo anime ${context.animePlatform.anime!!.name}")
            return
        }

        val show = episodes.first().show

        context.animeDatas.add(
            AnimeData(
                platform = context.animePlatform.platform!!,
                name = show.name,
                attachments = mapOf(
                    ImageType.BANNER to show.banner,
                    ImageType.CAROUSEL to show.carousel,
                    ImageType.TITLE to show.title,
                ),
                description = show.description
            )
        )
    }
}