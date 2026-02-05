package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Logger

class AniListMatchingJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var mailService: MailService

    private fun Logger.info(stringBuilder: StringBuilder, message: String) {
        this.info(message)
        stringBuilder.appendLine(message)
    }

    private fun Logger.warning(stringBuilder: StringBuilder, message: String) {
        this.warning(message)
        stringBuilder.appendLine(message)
    }

    private fun getSimulcastUuids(): List<UUID>? {
        val allUuids = simulcastCacheService.findAll().mapNotNull(SimulcastDto::uuid)
        val matchingSize = configCacheService.getValueAsInt(ConfigPropertyKey.ANILIST_SIMULCAST_MATCHING_SIZE, 1)

        return allUuids
            .let { if (matchingSize > 0) it.take(matchingSize) else it }
            .takeIfNotEmpty()
    }

    override suspend fun run() {
        val stringBuilder = StringBuilder()
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val simulcastUuids = getSimulcastUuids() ?: return

        val animes = CountryCode.entries.flatMap { countryCode ->
            animeService.findAllSimulcastedWithAnimePlatformInvalid(
                simulcastUuids,
                Platform.ANIL,
                zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.MATCHING_ANILIST_DELAY, 30).toLong()),
                countryCode.locale
            )
        }.distinctBy { it.uuid }

        logger.info(stringBuilder, "Found ${animes.size} animes to match")

        if (animes.isEmpty()) {
            logger.info(stringBuilder, "No animes found")
            return
        }

        val deprecatedAnimePlatformDateTime = zonedDateTime.minusMonths(configCacheService.getValueAsInt(ConfigPropertyKey.ANIME_PLATFORM_DEPRECATED_DURATION, 3).toLong())
        val needMatchingAnimes = animes.shuffled().take(configCacheService.getValueAsInt(ConfigPropertyKey.MATCHING_ANILIST_ANIME_SIZE, 5))
        var hasChange = false

        needMatchingAnimes.forEach { anime ->
            val shortName = StringUtils.getShortName(anime.name!!)
            logger.info(stringBuilder, "Matching anime $shortName...")

            val animePlatforms = animePlatformService.findAllByAnime(anime)
            val streamingPlatforms = animePlatforms.filter { it.platform!!.isStreamingPlatform }

            val anilistPlatforms = animePlatforms.filter { it.platform!! == Platform.ANIL }
            val media = AniListCachedWrapper.findAnilistMedia(anime.name!!, streamingPlatforms, anime.releaseDateTime.year)

            if (media == null) {
                logger.warning(stringBuilder, "No AniList media found for $shortName")
                deleteDeprecatedPlatforms(stringBuilder, shortName, anilistPlatforms, deprecatedAnimePlatformDateTime).onTrue { hasChange = true }

                if (AniListMatchingService.updateAnimeGenreAndTags(anime, shortName, null)) {
                    hasChange = true
                    animeService.update(anime)
                }

                return@forEach
            }

            logger.info(stringBuilder, "AniList media found with ID: ${media.id}")

            anilistPlatforms.singleOrNull { it.platformId == media.id.toString() }?.let {
                logger.info(stringBuilder, "Anime $shortName is already matched with AniList ID ${media.id}, validating...")
                it.lastValidateDateTime = zonedDateTime
                animePlatformService.update(it)
                traceActionService.createTraceAction(it, TraceAction.Action.UPDATE)
                return@forEach
            }

            logger.info(stringBuilder, "Anime $shortName has no match with AniList ID ${media.id}, creating it...")

            animePlatformService.save(AnimePlatform(
                anime = anime,
                platform = Platform.ANIL,
                platformId = media.id.toString(),
                lastValidateDateTime = zonedDateTime,
            )).apply {
                traceActionService.createTraceAction(this, TraceAction.Action.CREATE)
                hasChange = true
            }

            deleteDeprecatedPlatforms(stringBuilder, shortName, anilistPlatforms, deprecatedAnimePlatformDateTime).onTrue { hasChange = true }

            if (AniListMatchingService.updateAnimeGenreAndTags(anime, shortName, media)) {
                hasChange = true
                animeService.update(anime)
            }
        }

        logger.info(stringBuilder, "Matching job finished")

        if (hasChange) {
            mailService.saveAdminMail(
                title = "AniListMatchingJob - ${needMatchingAnimes.size} animes matched",
                body = stringBuilder.toString().replace("\n", "<br>")
            )

            InvalidationService.invalidate(AnimePlatform::class.java, Anime::class.java, Genre::class.java, AnimeTag::class.java, Tag::class.java)
        }
    }

    private fun deleteDeprecatedPlatforms(
        stringBuilder: StringBuilder,
        shortName: String,
        anilistPlatforms: List<AnimePlatform>,
        zonedDateTime: ZonedDateTime,
    ): Boolean {
        var hasDeleted = false

        anilistPlatforms.filter { it.lastValidateDateTime != null && it.lastValidateDateTime!!.isBeforeOrEqual(zonedDateTime) }
            .forEach {
                logger.warning(stringBuilder, "Deleting old anime platform ${it.platform} for anime $shortName with id ${it.platformId}")
                animePlatformService.delete(it)
                traceActionService.createTraceAction(it, TraceAction.Action.DELETE)
                hasDeleted = true
            }

        return hasDeleted
    }
}