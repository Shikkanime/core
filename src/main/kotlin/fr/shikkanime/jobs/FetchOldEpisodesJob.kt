package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.EpisodeNoSubtitlesOrVoiceException
import fr.shikkanime.platforms.*
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.services.caches.ConfigCacheService
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
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var configService: ConfigService
    @Inject private lateinit var configCacheService: ConfigCacheService
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

        val config = configService.findAllByName(ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key).firstOrNull() ?: run {
            logger.warning("Config ${ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key} not found")
            return
        }

        val to = LocalDate.parse(config.propertyValue!!)
        val from = to.minusDays(range.toLong())
        val dates = from.datesUntil(to.plusDays(1), Period.ofDays(1)).asSequence().toSortedSet()

        val episodes = mutableListOf<Episode>()
        val start = System.currentTimeMillis()

        logger.info("Fetching old episodes... (From ${dates.first()} to ${dates.last()})")

        runBlocking {
            episodes.addAll(CountryCode.entries.flatMap { fetchAnimationDigitalNetwork(it, dates) })
            episodes.addAll(CountryCode.entries.flatMap { fetchLiveChart(it, dates) })
        }

        val identifiers = episodeVariantService.findAllIdentifiers()
        episodes.removeIf { it.releaseDateTime.toLocalDate() < dates.min() || it.getIdentifier() in identifiers }

        if (episodes.isNotEmpty()) {
            val workbook = XSSFWorkbook()
            episodes.groupBy { it.platform }.forEach { (platform, episodes) ->
                val sheet = workbook.createSheet(platform.platformName)
                val rows = mutableListOf<Array<Any>>()

                rows.add(arrayOf(
                    "Country",
                    "Anime ID",
                    "Anime",
                    "Anime image",
                    "Anime banner",
                    "Anime carousel",
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
                        episode.animeAttachments[ImageType.THUMBNAIL] ?: StringUtils.EMPTY_STRING,
                        episode.animeAttachments[ImageType.BANNER] ?: StringUtils.EMPTY_STRING,
                        episode.animeAttachments[ImageType.CAROUSEL] ?: StringUtils.EMPTY_STRING,
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
                        when (val value = columns[j]) {
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

            val outputStream = FileOutputStream(
                File(
                    Constant.exportsFolder,
                    "old_episodes_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.xlsx"
                )
            )
            workbook.write(outputStream)
            workbook.close()
        }

        logger.info("Updating config to the next fetch date...")
        config.propertyValue = from.toString()
        configService.update(config)
        InvalidationService.invalidate(Config::class.java)
        traceActionService.createTraceAction(config, TraceAction.Action.UPDATE)

        logger.info("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
    }

    private suspend fun fetchAnimationDigitalNetwork(
        countryCode: CountryCode,
        dates: Set<LocalDate>
    ): List<Episode> {
        return dates.flatMap { date ->
            val zonedDateTime = date.atStartOfDay(Constant.utcZoneId)
            try {
                AnimationDigitalNetworkCachedWrapper.getLatestVideos(countryCode, zonedDateTime.toLocalDate()).flatMap { video ->
                    try {
                        animationDigitalNetworkPlatform.convertEpisode(
                            countryCode, video, zonedDateTime, false
                        )
                    } catch (e: EpisodeNoSubtitlesOrVoiceException) {
                        logger.warning("Error while fetching ADN episodes: ${e.message}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while processing episodes for date $date", e)
                emptyList()
            }
        }
    }

    private suspend fun fetchLiveChart(countryCode: CountryCode, dates: Set<LocalDate>): List<Episode> {
        val minimalDate = dates.min()
        val startOfWeekDates = dates.map { it.atStartOfWeek() }.toSet()
        val animeIds = startOfWeekDates.flatMap { HttpRequest.retry(3, delay = 5_000) { LiveChartCachedWrapper.getAnimeIdsFromDate(it) }.toList() }.toSet()
        val ids = animeIds.associateWith { LiveChartCachedWrapper.getStreamsByAnimeId(it).filterNot { entry -> entry.key == Platform.ANIM } }

        return ids.flatMap { (_, platformIds) ->
            platformIds.flatMap { (platform, ids) ->
                ids.flatMap { id ->
                    when (platform) {
                        Platform.CRUN -> CrunchyrollCachedWrapper.getEpisodesBySeriesId(countryCode.locale, id)
                            .mapNotNull { episode ->
                                runCatching { crunchyrollPlatform.convertEpisode(countryCode, episode, false) }.getOrNull()
                            }
                        Platform.DISN -> DisneyPlusCachedWrapper.getEpisodesByShowId(
                            countryCode, id,
                            configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_DISNEY_PLUS_AUDIO_LOCALES)
                        ).flatMap { episode ->
                            runCatching {
                                disneyPlusPlatform.convertEpisode(
                                    countryCode, episode, minimalDate.atStartOfDay(ZoneId.of(countryCode.timezone))
                                )
                            }.getOrElse { emptyList() }
                        }
                        Platform.NETF -> NetflixCachedWrapper.getEpisodesByShowId(countryCode, id.toInt())
                            .flatMap { episode ->
                                val audioLocales = episode.audioLocales.ifEmpty { setOf("ja-JP") }
                                audioLocales.mapNotNull { audioLocale ->
                                    runCatching {
                                        netflixPlatform.convertEpisode(
                                            countryCode, StringUtils.EMPTY_STRING, episode, audioLocale
                                        )
                                    }.getOrNull()
                                }
                            }
                        Platform.PRIM -> HttpRequest.retry(3) {
                            PrimeVideoCachedWrapper.getEpisodesByShowId(countryCode, id)
                                .flatMap { episode ->
                                    runCatching {
                                        primeVideoPlatform.convertEpisode(
                                            countryCode, StringUtils.EMPTY_STRING, episode,
                                            minimalDate.atStartOfDay(ZoneId.of(countryCode.timezone))
                                        )
                                    }.getOrElse { emptyList() }
                                }
                        }
                        else -> emptyList()
                    }
                }
            }
        }
    }
}