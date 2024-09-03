package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.Period
import java.util.concurrent.TimeoutException
import java.util.logging.Level

class FetchOldEpisodesJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform

    @Inject
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var traceActionService: TraceActionService

    override fun run() {
        val range = configCacheService.getValueAsIntNullable(ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE) ?: run {
            logger.warning("Config ${ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key} not found")
            return
        }

        if (range == -1) {
            logger.warning("FetchOldEpisodesJob is disabled")
            return
        }

        val config = configService.findByName(ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key) ?: run {
            logger.warning("Config ${ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key} not found")
            return
        }

        val to = LocalDate.parse(config.propertyValue!!)
        val from = to.minusDays(range.toLong())
        val dates = from.datesUntil(to.plusDays(1), Period.ofDays(1)).toList().sorted()

        val episodes = mutableListOf<Episode>()
        val start = System.currentTimeMillis()
        logger.info("Fetching old episodes... (From ${dates.first()} to ${dates.last()})")

        episodes.addAll(
            animationDigitalNetworkPlatform.configuration?.availableCountries?.flatMap {
                fetchAnimationDigitalNetwork(it, dates)
            } ?: emptyList()
        )

        episodes.addAll(
            crunchyrollPlatform.configuration?.availableCountries?.flatMap {
                try {
                    fetchCrunchyroll(it, dates)
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while fetching Crunchyroll episodes", e)
                    return
                }
            } ?: emptyList()
        )

        episodes.removeIf { it.releaseDateTime.toLocalDate() !in dates || it.episodeType == EpisodeType.FILM }

        val limit = configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT, 5)

        if (limit != -1) {
            episodes.groupBy { it.anime + it.releaseDateTime.toLocalDate().toString() }.forEach { (_, animeDayEpisodes) ->
                if (animeDayEpisodes.size > limit) {
                    logger.warning("More than $limit episodes for ${animeDayEpisodes.first().anime} on ${animeDayEpisodes.first().releaseDateTime.toLocalDate()}, removing...")
                    episodes.removeAll(animeDayEpisodes)
                    return@forEach
                }
            }
        }

        logger.info("Found ${episodes.size} episodes, saving...")
        var realSaved = 0
        val realSavedAnimes = mutableSetOf<String>()

        episodes.sortedBy { it.releaseDateTime }.forEach { episode ->
            val findByIdentifier = episodeVariantService.findByIdentifier(episode.getIdentifier())

            if (findByIdentifier == null) {
                realSavedAnimes.add(episode.anime)
                realSaved++
                episodeVariantService.save(episode, false)
            }
        }

        logger.info("Saved $realSaved episodes")
        realSavedAnimes.forEach { logger.info("Updating $it...") }

        if (realSaved > 0) {
            logger.info("Recalculating simulcasts...")
            animeService.recalculateSimulcasts()

            MapCache.invalidate(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                Simulcast::class.java
            )
        }

        logger.info("Updating config to the next fetch date...")
        config.propertyValue = from.toString()
        configService.update(config)
        traceActionService.createTraceAction(config, TraceAction.Action.UPDATE)
        logger.info("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
    }

    private fun fetchAnimationDigitalNetwork(
        countryCode: CountryCode,
        dates: List<LocalDate>
    ): List<Episode> {
        val episodes = mutableListOf<Episode>()

        dates.forEachIndexed { _, date ->
            val zonedDateTime = date.atStartOfDay(Constant.utcZoneId)

            runBlocking {
                animationDigitalNetworkPlatform.fetchApiContent(
                    countryCode,
                    zonedDateTime
                )
            }.forEach { video ->
                try {
                    episodes.addAll(
                        animationDigitalNetworkPlatform.convertEpisode(
                            countryCode,
                            video,
                            zonedDateTime,
                            false
                        )
                    )
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error while converting episode (Episode ID: ${video.id})",
                        e
                    )
                }
            }
        }

        return episodes
    }

    private fun fetchCrunchyroll(
        countryCode: CountryCode,
        dates: List<LocalDate>
    ): List<Episode> {
        val episodes = mutableListOf<Episode>()

        dates.map { it.atStartOfWeek() }
            .distinct()
            .forEachIndexed { _, date ->
                try {
                    tryFetchCrunchyrollCalendar(countryCode, date)
                        .forEach { browseObject ->
                            try {
                                episodes.add(
                                    crunchyrollPlatform.convertEpisode(
                                        countryCode,
                                        browseObject,
                                        false
                                    )
                                )
                            } catch (e: Exception) {
                                logger.log(
                                    Level.SEVERE,
                                    "Error while converting episode (Episode ID: ${browseObject.id})",
                                    e
                                )
                            }
                        }
                } catch (e: Exception) {
                    throw e
                }
            }

        return episodes
    }

    private fun tryFetchCrunchyrollCalendar(
        countryCode: CountryCode,
        date: LocalDate
    ): Array<CrunchyrollWrapper.BrowseObject> {
        var retry = 0

        while (true) {
            try {
                return runBlocking {
                    CrunchyrollWrapper.getSimulcastCalendar(
                        countryCode,
                        crunchyrollPlatform.identifiers[countryCode]!!,
                        date
                    )
                }
            } catch (e: Exception) {
                if (++retry > 3) {
                    logger.log(Level.SEVERE, "Error while fetching Crunchyroll calendar", e)
                    throw TimeoutException("Error while fetching Crunchyroll calendar")
                }

                logger.log(Level.WARNING, "Error while fetching Crunchyroll calendar, retrying... (Retry: $retry/3)")
            }
        }
    }
}