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
import fr.shikkanime.exceptions.EpisodeNotAvailableInCountryException
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.util.logging.Level

class FetchOldEpisodesJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var animationDigitalNetworkPlatform: AnimationDigitalNetworkPlatform

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var episodeCacheService: ConfigCacheService

    override fun run() {
        val config = configService.findByName(ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key) ?: run {
            logger.warning("Config ${ConfigPropertyKey.LAST_FETCH_OLD_EPISODES.key} not found")
            return
        }

        val range = episodeCacheService.getValueAsInt(ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE)

        if (range == -1) {
            logger.warning("Config ${ConfigPropertyKey.FETCH_OLD_EPISODES_RANGE.key} not found")
            return
        }

        val to = LocalDate.parse(config.propertyValue!!)
        val from = to.minusDays(range.toLong())
        val dates = from.datesUntil(to.plusDays(1), Period.ofDays(1)).toList().sorted()
        val simulcasts = dates.map {
            "${Constant.seasons[(it.monthValue - 1) / 3]}-${it.year}".lowercase().replace("autumn", "fall")
        }.toSet()
        val episodes = mutableListOf<Episode>()
        val start = System.currentTimeMillis()
        logger.info("Fetching old episodes... (From ${dates.first()} to ${dates.last()})")

        episodes.addAll(fetchAnimationDigitalNetwork(CountryCode.FR, dates))
        episodes.addAll(runBlocking { fetchCrunchyroll(CountryCode.FR, simulcasts) })

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

        val variants = episodes.sortedBy { it.releaseDateTime }.map { episode ->
            episodeVariantService.findByIdentifier(episode.getIdentifier()) ?: run {
                realSaved++
                episodeVariantService.save(episode)
            }
        }

        logger.info("Saved $realSaved episodes")

        if (realSaved > 0) {
            logger.info("Updating mappings...")

            variants.groupBy { it.mapping!!.uuid }.forEach { (mappingUuid, variants) ->
                val mapping = episodeMappingService.find(mappingUuid) ?: return@forEach
                mapping.releaseDateTime = variants.minOf { it.releaseDateTime }
                mapping.lastReleaseDateTime = variants.maxOf { it.releaseDateTime }
                episodeMappingService.update(mapping)
            }

            logger.info("Updating animes...")

            variants.groupBy { it.mapping!!.anime!!.uuid }.forEach { (animeUuid, variants) ->
                val anime = animeService.find(animeUuid) ?: return@forEach
                logger.info("Updating ${StringUtils.getShortName(anime.name!!)}...")
                anime.releaseDateTime = variants.minOf { it.releaseDateTime }
                anime.lastReleaseDateTime = variants.maxOf { it.releaseDateTime }
                animeService.update(anime)
            }

            logger.info("Updating simulcasts...")
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
            }.forEach { episodeJson ->
                try {
                    episodes.addAll(
                        animationDigitalNetworkPlatform.convertEpisode(
                            countryCode,
                            episodeJson.asJsonObject,
                            zonedDateTime,
                            false
                        )
                    )
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error while converting episode (Episode ID: ${episodeJson.getAsString("id")})",
                        e
                    )
                }
            }
        }

        return episodes
    }

    val crunchyrollEpisodesCache =
        MapCache<CountryCodeIdKeyCache, List<CrunchyrollWrapper.Episode>>(duration = Duration.ofDays(7)) {
            try {
                val accessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
                val seasons =
                    runBlocking { CrunchyrollWrapper.getSeasonsBySeriesId(it.countryCode.locale, accessToken, it.id) }
                        .filter { season -> season.subtitleLocales.contains(it.countryCode.locale) }

                return@MapCache seasons.flatMap { season ->
                    runBlocking {
                        CrunchyrollWrapper.getEpisodesBySeasonId(
                            it.countryCode.locale,
                            accessToken,
                            season.id
                        )
                    }
                        .flatMap { it.versions ?: listOf(CrunchyrollWrapper.Version(it.id)) }
                        .distinctBy { it.guid }
                        .parallelStream().map { version ->
                            runBlocking {
                                CrunchyrollWrapper.getEpisode(
                                    it.countryCode.locale,
                                    accessToken,
                                    version.guid
                                )
                            }
                        }.toList()
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while fetching Crunchyroll episodes", e)
                return@MapCache emptyList()
            }
        }

    private suspend fun fetchCrunchyroll(countryCode: CountryCode, simulcasts: Set<String>): List<Episode> {
        val accessToken = CrunchyrollWrapper.getAnonymousAccessToken()
        val platformEpisodes = mutableListOf<Episode>()

        val series = simulcasts.flatMap { simulcastId ->
            CrunchyrollWrapper.getBrowse(
                countryCode.locale,
                accessToken,
                sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                type = CrunchyrollWrapper.MediaType.SERIES,
                200,
                simulcast = simulcastId
            )
        }.distinctBy { it.getAsString("id") }

        series.forEach { serie ->
            val animeName = requireNotNull(serie.getAsString("title")) { "Anime name is null" }
            val images = serie.getAsJsonObject("images")
            val postersTall = images.getAsJsonArray("poster_tall")[0].asJsonArray
            val postersWide = images.getAsJsonArray("poster_wide")[0].asJsonArray
            val animeImage =
                postersTall?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                    "source"
                )
            val animeBanner =
                postersWide?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                    "source"
                )
            val animeDescription = serie.getAsString("description")

            if (animeImage.isNullOrEmpty()) {
                throw Exception("Image is null or empty")
            }
            if (animeBanner.isNullOrEmpty()) {
                throw Exception("Banner is null or empty")
            }

            crunchyrollEpisodesCache[CountryCodeIdKeyCache(
                countryCode,
                serie.getAsString("id")!!
            )]?.forEach { episode ->
                try {
                    val isDubbed = episode.audioLocale == countryCode.locale
                    val releaseDate = ZonedDateTime.parse(episode.premiumAvailableDate)
                    val season = episode.seasonNumber ?: 1
                    val (number, episodeType) = getNumberAndEpisodeType(episode)
                    val url = CrunchyrollWrapper.buildUrl(countryCode, episode.id, episode.slugTitle)
                    val thumbnails = episode.images?.thumbnail
                    val biggestImage = thumbnails?.get(0)?.maxByOrNull { it.width }
                    val image = biggestImage?.source?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_IMAGE_PREVIEW
                    val duration = episode.durationMs / 1000
                    val description = episode.description?.replace('\n', ' ')?.takeIf { it.isNotBlank() }

                    if (!episode.eligibleRegion.contains(countryCode.name))
                        throw EpisodeNotAvailableInCountryException("Episode of $animeName is not available in ${countryCode.name}")

                    if (!isDubbed && (episode.subtitleLocales.isEmpty() || !episode.subtitleLocales.contains(countryCode.locale)))
                        throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

                    platformEpisodes.add(
                        Episode(
                            countryCode = countryCode,
                            anime = animeName,
                            animeImage = animeImage,
                            animeBanner = animeBanner,
                            animeDescription = animeDescription,
                            releaseDateTime = releaseDate,
                            episodeType = episodeType,
                            season = season,
                            number = number,
                            duration = duration,
                            title = episode.title,
                            description = description,
                            image = image,
                            platform = Platform.CRUN,
                            audioLocale = episode.audioLocale,
                            id = episode.id,
                            url = url,
                            uncensored = false,
                        )
                    )
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error while converting episode (Episode ID: ${episode.id})",
                        e
                    )
                }
            }
        }

        return platformEpisodes
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