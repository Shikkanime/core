package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.EpisodeNoSubtitlesOrVoiceException
import fr.shikkanime.platforms.*
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.impl.caches.*
import kotlinx.coroutines.runBlocking
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import kotlin.streams.asSequence

class FetchOldEpisodesJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform
    @Inject private lateinit var crunchyrollPlatform: CrunchyrollPlatform
    @Inject private lateinit var disneyPlusPlatform: DisneyPlusPlatform
    @Inject private lateinit var netflixPlatform: NetflixPlatform
    @Inject private lateinit var primeVideoPlatform: PrimeVideoPlatform

    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var configService: ConfigService
    @Inject private lateinit var traceActionService: TraceActionService

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

        logger.info("Fetching old episodes... (From ${dates.first()} to ${dates.last()})")

        episodes.addAll(
            animationDigitalNetworkPlatform.configuration?.availableCountries?.flatMap {
                fetchAnimationDigitalNetwork(it, dates)
            } ?: emptyList()
        )

        episodes.addAll(
            CountryCode.entries.flatMap {
                runBlocking { fetchLiveChart(it, dates) }
            }
        )

        val identifiers = episodeVariantCacheService.findAllIdentifiers()
        episodes.removeIf { it.releaseDateTime.toLocalDate() < dates.min() || it.getIdentifier() in identifiers }

        if (episodes.isNotEmpty()) {
            // Save into a Excel sheet
            val workbook = XSSFWorkbook()
            // Create a sheet per platform
            episodes.groupBy { it.platform }.forEach { (platform, episodes) ->
                val sheet = workbook.createSheet(platform.platformName)
                val rows = mutableListOf<Array<Any>>()

                rows.add(arrayOf(
                    "Country",
                    "Anime ID",
                    "Anime",
                    "Anime image",
                    "Anime banner",
                    "Anime description",
                    "Release date time",
                    "Episode type",
                    "Season ID",
                    "Season",
                    "Number",
                    "Duration",
                    "Title",
                    "Description",
                    "Image",
                    "Platform",
                    "Audio locale",
                    "ID",
                    "URL",
                    "Uncensored",
                    "Original"
                ))

                episodes.forEach { episode ->
                    rows.add(arrayOf(
                        episode.countryCode,
                        episode.animeId,
                        episode.anime,
                        episode.animeImage,
                        episode.animeBanner,
                        episode.animeDescription?.normalize() ?: StringUtils.EMPTY_STRING,
                        episode.releaseDateTime,
                        episode.episodeType,
                        episode.seasonId,
                        episode.season,
                        episode.number,
                        episode.duration,
                        episode.title?.normalize() ?: StringUtils.EMPTY_STRING,
                        episode.description?.normalize() ?: StringUtils.EMPTY_STRING,
                        episode.image,
                        episode.platform,
                        episode.audioLocale,
                        episode.id,
                        episode.url,
                        episode.uncensored,
                        episode.original
                    ))
                }

                for (i in rows.indices) {
                    val row = sheet.createRow(i)
                    val columns = rows[i]
                    for (j in columns.indices) {
                        val cell = row.createCell(j)
                        val value = columns[j]

                        when (value) {
                            is ZonedDateTime -> {
                                cell.setCellValue(value.withUTC().toLocalDateTime())
                                val cellStyle = workbook.createCellStyle()
                                cellStyle.dataFormat = workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss")
                                cell.cellStyle = cellStyle
                            }
                            is Number -> cell.setCellValue(value.toDouble())
                            else -> cell.setCellValue(value.toString())
                        }
                    }
                }
            }

            val outputStream = FileOutputStream(File(Constant.exportsFolder, "old_episodes_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.xlsx"))
            workbook.write(outputStream)
            workbook.close()
        }

        logger.info("Updating config to the next fetch date...")

        config.propertyValue = from.toString()
        configService.update(config)
        MapCache.invalidate(Config::class.java)
        traceActionService.createTraceAction(config, TraceAction.Action.UPDATE)

        logger.info("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
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

    private suspend fun fetchLiveChart(countryCode: CountryCode, dates: Set<LocalDate>): List<Episode> {
        val minimalDate = dates.min()
        val startOfWeekDates = dates.map { it.atStartOfWeek() }.toSet()
        val animeIds = startOfWeekDates.flatMap { LiveChartCachedWrapper.getAnimeIdsFromDate(it) }.toSet()
        // Remove ids from ADN (ANIM)
        val ids = animeIds.associateWith { LiveChartCachedWrapper.getStreamsForAnime(it).filterNot { entry -> entry.key == Platform.ANIM } }
        val episodes = mutableListOf<Episode>()

        ids.forEach { (_, platformIds) ->
            platformIds.forEach { (platform, ids) ->
                ids.forEach { id ->
                    when (platform) {
                        Platform.ANIM -> {
                            AnimationDigitalNetworkCachedWrapper.getShowVideos(id.toInt()).forEach { video ->
                                runCatching {
                                    animationDigitalNetworkPlatform.convertEpisode(
                                        countryCode,
                                        video,
                                        minimalDate.atStartOfDay(Constant.utcZoneId),
                                        false
                                    )
                                }.onSuccess {
                                    episodes.addAll(it)
                                }
                            }
                        }
                        Platform.CRUN -> {
                            CrunchyrollCachedWrapper.getEpisodesBySeriesId(countryCode.locale, id)
                                .forEach { episode ->
                                    runCatching {
                                        crunchyrollPlatform.convertEpisode(
                                            countryCode,
                                            episode,
                                            false
                                        )
                                    }.onSuccess {
                                        episodes.add(it)
                                    }
                                }
                        }
                        Platform.DISN -> {
                            DisneyPlusCachedWrapper.getEpisodesByShowId(
                                countryCode.locale,
                                id,
                                configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
                            ).forEach { episode ->
                                runCatching {
                                    disneyPlusPlatform.convertEpisode(
                                        countryCode,
                                        episode,
                                        minimalDate.atStartOfDay(ZoneId.of(countryCode.timezone))
                                    )
                                }.onSuccess {
                                    episodes.addAll(it)
                                }
                           }
                        }
                        Platform.NETF -> {
                            NetflixCachedWrapper.getEpisodesByShowId(
                                countryCode.locale,
                                id.toInt()
                            ).forEach { episode ->
                                runCatching {
                                    netflixPlatform.convertEpisode(
                                        countryCode,
                                        StringUtils.EMPTY_STRING,
                                        episode,
                                        EpisodeType.EPISODE,
                                        "ja-JP"
                                    )
                                }.onSuccess {
                                    episodes.add(it)
                                }
                            }
                        }
                        Platform.PRIM -> {
                            PrimeVideoCachedWrapper.getEpisodesByShowId(
                                countryCode.locale,
                                id
                            ).forEach { episode ->
                                runCatching {
                                    primeVideoPlatform.convertEpisode(
                                        countryCode,
                                        StringUtils.EMPTY_STRING,
                                        episode,
                                        minimalDate.atStartOfDay(ZoneId.of(countryCode.timezone)),
                                        EpisodeType.EPISODE
                                    )
                                }.onSuccess {
                                    episodes.addAll(it)
                                }
                            }
                        }
                    }
                }
            }
        }

        return episodes
    }
}