package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.EpisodeNoSubtitlesOrVoiceException
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.Period
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

    private val crunchyrollAccessTokenCache = MapCache<CountryCode, String>(duration = Duration.ofMinutes(30)) {
        runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
    }

    override fun run() {
        val config = configService.findByName(ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key) ?: run {
            logger.warning("Config ${ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key} not found")
            return
        }

        val range = configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE)

        if (range == -1) {
            logger.warning("Config ${ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key} not found")
            return
        }

        val to = LocalDate.parse(config.propertyValue!!)
        val from = to.minusDays(range.toLong())
        val dates = from.datesUntil(to.plusDays(1), Period.ofDays(1)).toList().sorted()
        val simulcasts = getSimulcasts(dates)

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
                runBlocking { fetchCrunchyroll(it, simulcasts) }
            } ?: emptyList()
        )

        episodes.removeIf { it.releaseDateTime.toLocalDate() !in dates || it.episodeType == EpisodeType.FILM }

        val limit = configCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODES_LIMIT, 5)
        episodes.groupBy { it.anime + it.releaseDateTime.toLocalDate().toString() }.forEach { (_, animeDayEpisodes) ->
            if (animeDayEpisodes.size > limit) {
                logger.warning("More than $limit episodes for ${animeDayEpisodes.first().anime} on ${animeDayEpisodes.first().releaseDateTime.toLocalDate()}, removing...")
                episodes.removeAll(animeDayEpisodes)
                return@forEach
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
        realSavedAnimes.forEach { logger.info("Updating ${StringUtils.getShortName(it)}...") }

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
        logger.info("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
    }

    fun getSimulcasts(dates: List<LocalDate>): Set<String> {
        val simulcastRange = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE, 1)
        val simulcastDates = dates.toMutableSet()

        for (i in 1..simulcastRange) {
            simulcastDates.addAll(dates.map { it.plusDays(i.toLong()) })
            simulcastDates.addAll(dates.map { it.minusDays(i.toLong()) })
        }

        return simulcastDates.sorted().map {
            "${Constant.seasons[(it.monthValue - 1) / 3]}-${it.year}".lowercase().replace("autumn", "fall")
        }.toSet()
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

    val crunchyrollEpisodesCache =
        MapCache<CountryCodeIdKeyCache, List<CrunchyrollWrapper.BrowseObject>>(duration = Duration.ofDays(7)) {
            runBlocking {
                try {
                    val episodes = mutableListOf<CrunchyrollWrapper.BrowseObject>()

                    val crEpisodes = CrunchyrollWrapper.getSeasonsBySeriesId(it.countryCode.locale, crunchyrollAccessTokenCache[it.countryCode]!!, it.id)
                        .filter { season -> season.subtitleLocales.contains(it.countryCode.locale) }
                        .parallelStream().map { season ->
                            try {
                                runBlocking {
                                    CrunchyrollWrapper.getEpisodesBySeasonId(
                                        it.countryCode.locale,
                                        crunchyrollAccessTokenCache[it.countryCode]!!,
                                        season.id
                                    ).toList()
                                }
                            } catch (e: Exception) {
                                logger.warning("Error while fetching Crunchyroll episodes by season (Season ID: ${season.id}) : ${e.message}")
                                emptyList()
                            }
                        }.toList()
                        .flatten()

                    // Need to list all available variants
                    val variants = mutableSetOf<CrunchyrollWrapper.Version>()
                    variants.addAll(crEpisodes.flatMap { it.versions ?: listOf(CrunchyrollWrapper.Version(it.id!!)) })

                    // Remove duplicates and already fetched episodes
                    val chunked = variants.distinctBy { variant -> variant.guid }
                        .chunked(50)

                    chunked.parallelStream().forEach { chunkedVariants ->
                        try {
                            runBlocking {
                                episodes.addAll(
                                    CrunchyrollWrapper.getObjects(
                                        it.countryCode.locale,
                                        crunchyrollAccessTokenCache[it.countryCode]!!,
                                        *chunkedVariants.map { it.guid }.toTypedArray()
                                    ).toList()
                                )
                            }
                        } catch (e: Exception) {
                            logger.warning("Error while fetching Crunchyroll chunked variants: ${e.message}")
                        }
                    }

                    return@runBlocking episodes
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while fetching Crunchyroll episodes", e)
                    return@runBlocking emptyList()
                }
            }
        }

    private suspend fun fetchCrunchyroll(countryCode: CountryCode, simulcasts: Set<String>): List<Episode> {
        val platformEpisodes = mutableListOf<Episode>()

        val series = getSeries(countryCode, simulcasts)

        series.forEach { serie ->
            val seasonRegex = " Saison (\\d)".toRegex()

            var animeName = requireNotNull(serie.title) { "Anime name is null" }
            var forcedSeason: Int? = null

            if (animeName.contains(seasonRegex)) {
                forcedSeason = seasonRegex.find(animeName)!!.groupValues[1].toIntOrNull()
                animeName = animeName.replace(seasonRegex, "")
            }

            val animeImage = serie.images!!.posterTall.first().maxByOrNull { poster -> poster.width }?.source
            val animeBanner = serie.images.posterWide.first().maxByOrNull { poster -> poster.width }?.source
            val animeDescription = serie.description

            if (animeImage.isNullOrEmpty()) {
                throw Exception("Image is null or empty")
            }
            if (animeBanner.isNullOrEmpty()) {
                throw Exception("Banner is null or empty")
            }

            crunchyrollEpisodesCache[CountryCodeIdKeyCache(countryCode, serie.id)]?.forEach { episode ->
                try {
                    val isDubbed = episode.episodeMetadata!!.audioLocale == countryCode.locale
                    val season = forcedSeason ?: (episode.episodeMetadata.seasonNumber ?: 1)
                    val (number, episodeType) = getNumberAndEpisodeType(episode.episodeMetadata)
                    val url = CrunchyrollWrapper.buildUrl(countryCode, episode.id, episode.slugTitle)
                    val biggestImage = episode.images?.thumbnail?.firstOrNull()?.maxByOrNull { it.width }
                    val image = biggestImage?.source?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_IMAGE_PREVIEW
                    val duration = episode.episodeMetadata.durationMs / 1000
                    val description = episode.description?.replace('\n', ' ')?.takeIf { it.isNotBlank() }

                    if (!isDubbed && (episode.episodeMetadata.subtitleLocales.isEmpty() || !episode.episodeMetadata.subtitleLocales.contains(
                            countryCode.locale
                        ))
                    )
                        throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

                    platformEpisodes.add(
                        Episode(
                            countryCode = countryCode,
                            anime = animeName,
                            animeImage = animeImage,
                            animeBanner = animeBanner,
                            animeDescription = animeDescription,
                            releaseDateTime = episode.episodeMetadata.premiumAvailableDate,
                            episodeType = episodeType,
                            season = season,
                            number = number,
                            duration = duration,
                            title = episode.title,
                            description = description,
                            image = image,
                            platform = Platform.CRUN,
                            audioLocale = episode.episodeMetadata.audioLocale,
                            id = episode.id,
                            url = url,
                            uncensored = false,
                        )
                    )
                } catch (e: Exception) {
                    logger.warning("Error while converting episode (Episode ID: ${episode.id}) : ${e.message}")
                }
            }
        }

        return platformEpisodes
    }

    suspend fun getSeries(
        countryCode: CountryCode,
        simulcasts: Set<String>,
    ): List<CrunchyrollWrapper.BrowseObject> {
        return simulcasts.flatMap { simulcastId ->
            CrunchyrollWrapper.getBrowse(
                countryCode.locale,
                crunchyrollAccessTokenCache[countryCode]!!,
                sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                type = CrunchyrollWrapper.MediaType.SERIES,
                200,
                simulcast = simulcastId
            ).toList()
        }.distinctBy { it.id }
    }

    private fun getNumberAndEpisodeType(episode: CrunchyrollWrapper.Episode): Pair<Int, EpisodeType> {
        var number = episode.number ?: -1
        val specialEpisodeRegex = "SP(\\d*)".toRegex()

        var episodeType = when {
            episode.seasonSlugTitle?.contains("movie", true) == true -> EpisodeType.FILM
            number == -1 -> EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        specialEpisodeRegex.find(episode.numberString)?.let {
            episodeType = EpisodeType.SPECIAL
            number = it.groupValues[1].toIntOrNull() ?: -1
        }

        return Pair(number, episodeType)
    }
}