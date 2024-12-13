package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.exceptions.EpisodeNoSubtitlesOrVoiceException
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.Period
import java.util.logging.Level
import kotlin.streams.asSequence

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
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var emailService: EmailService

    private fun log(stringBuilder: StringBuilder, level: Level, message: String) {
        logger.log(level, message)
        stringBuilder.append("[${level.localizedName}] $message<br/>")
    }

    override fun run() {
        val range = configCacheService.getValueAsIntNullable(ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE) ?: run {
            logger.warning("Config ${ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key} not found")
            return
        }

        if (range == -1) {
            logger.warning("FetchOldEpisodesJob is disabled")
            return
        }

        val config = configCacheService.findByName(ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key) ?: run {
            logger.warning("Config ${ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key} not found")
            return
        }

        val to = LocalDate.parse(config.propertyValue!!)
        val from = to.minusDays(range.toLong())
        val dates = from.datesUntil(to.plusDays(1), Period.ofDays(1)).asSequence().sorted().toSet()

        val episodes = mutableListOf<Episode>()
        val start = System.currentTimeMillis()
        val emailLogs = StringBuilder()

        log(emailLogs, Level.INFO, "Fetching old episodes... (From ${dates.first()} to ${dates.last()})")

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
                    return@flatMap emptyList<Episode>()
                }
            } ?: emptyList()
        )

        episodes.removeIf { it.releaseDateTime.toLocalDate() !in dates }

        val limit = configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT, 5)

        if (limit != -1) {
            episodes.groupBy { it.anime + it.releaseDateTime.toLocalDate().toString() }.forEach { (_, animeDayEpisodes) ->
                if (animeDayEpisodes.size > limit) {
                    log(emailLogs, Level.WARNING, "More than $limit episodes for ${animeDayEpisodes.first().anime} on ${animeDayEpisodes.first().releaseDateTime.toLocalDate()}, removing...")
                    episodes.removeAll(animeDayEpisodes)
                    return@forEach
                }
            }
        }

        log(emailLogs, Level.INFO, "Found ${episodes.size} episodes, saving...")
        var realSaved = 0
        val realSavedAnimes = mutableSetOf<String>()
        val identifiers = episodeVariantCacheService.findAllIdentifiers()

        episodes.sortedBy { it.releaseDateTime }.forEach { episode ->
            if (identifiers.none { it == episode.getIdentifier() }) {
                realSavedAnimes.add(StringUtils.getShortName(episode.anime))
                realSaved++
                episodeVariantService.save(episode, false)
            }
        }

        log(emailLogs, Level.INFO, "Saved $realSaved episodes")

        realSavedAnimes.forEach {
            log(emailLogs, Level.INFO, "Updating $it...")
        }

        if (realSaved > 0) {
            log(emailLogs, Level.INFO, "Recalculating simulcasts...")
            animeService.recalculateSimulcasts()

            MapCache.invalidate(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                Simulcast::class.java
            )
        }

        log(emailLogs, Level.INFO, "Updating config to the next fetch date...")

        config.propertyValue = from.toString()
        configService.update(config)
        MapCache.invalidate(Config::class.java)
        traceActionService.createTraceAction(config, TraceAction.Action.UPDATE)

        log(emailLogs, Level.INFO, "Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")

        emailService.sendAdminEmail(
            "FetchOldEpisodesJob - ${dates.first()} to ${dates.last()}",
            emailLogs.toString(),
        )
    }

    private fun fetchAnimationDigitalNetwork(
        countryCode: CountryCode,
        dates: Set<LocalDate>
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
                } catch (e: EpisodeNoSubtitlesOrVoiceException) {
                    logger.warning("Error while fetching ADN episodes: ${e.message}")
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
        dates: Set<LocalDate>
    ): List<Episode> {
        return CrunchyrollCachedWrapper.getSimulcastCalendarWithDates(
            countryCode,
            dates
        ).mapNotNull { browseObject ->
            try {
                crunchyrollPlatform.convertEpisode(
                    countryCode,
                    browseObject,
                    false
                )
            } catch (e: EpisodeNoSubtitlesOrVoiceException) {
                logger.warning("Error while fetching Crunchyroll episodes: ${e.message}")
                null
            } catch (e: Exception) {
                logger.log(
                    Level.SEVERE,
                    "Error while converting episode (Episode ID: ${browseObject.id})",
                    e
                )
                null
            }
        }
    }
}