package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.platforms.CrunchyrollPlatform
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
import java.time.LocalDate
import java.time.Period
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
    private lateinit var crunchyrollPlatform: CrunchyrollPlatform

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
        val episodes = mutableListOf<AbstractPlatform.Episode>()
        val start = System.currentTimeMillis()
        logger.info("Fetching old episodes... (From ${dates.first()} to ${dates.last()})")

        episodes.addAll(fetchAnimationDigitalNetwork(CountryCode.FR, dates))
        episodes.addAll(fetchCrunchyroll(CountryCode.FR, simulcasts))

        episodes.removeIf { it.releaseDateTime.toLocalDate() !in dates }

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
        logger.info("Updating config to the next fetch date...")
        config.propertyValue = from.toString()
        configService.update(config)
        logger.info("Take ${(System.currentTimeMillis() - start) / 1000}s to check ${dates.size} dates")
    }

    private fun fetchAnimationDigitalNetwork(
        countryCode: CountryCode,
        dates: List<LocalDate>
    ): List<AbstractPlatform.Episode> {
        val episodes = mutableListOf<AbstractPlatform.Episode>()

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

    private fun fetchCrunchyroll(countryCode: CountryCode, simulcasts: Set<String>): List<AbstractPlatform.Episode> {
        val accessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(accessToken) }
        val episodes = mutableListOf<AbstractPlatform.Episode>()

        val series = simulcasts.flatMap { simulcastId ->
            runBlocking {
                CrunchyrollWrapper.getBrowse(
                    countryCode.locale,
                    accessToken,
                    sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                    type = CrunchyrollWrapper.MediaType.SERIES,
                    100,
                    simulcast = simulcastId
                )
            }
        }

        val titles = series.map { jsonObject -> jsonObject.getAsString("title")!!.lowercase() }.toSet()
        val ids = series.map { jsonObject -> jsonObject.getAsString("id")!! }.toSet()

        crunchyrollPlatform.simulcasts[CountryCode.FR] = titles

        series.forEach {
            val postersTall = it.getAsJsonObject("images").getAsJsonArray("poster_tall")[0].asJsonArray
            val postersWide = it.getAsJsonObject("images").getAsJsonArray("poster_wide")[0].asJsonArray
            val image =
                postersTall?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                    "source"
                )!!
            val banner =
                postersWide?.maxByOrNull { poster -> poster.asJsonObject.getAsInt("width")!! }?.asJsonObject?.getAsString(
                    "source"
                )!!
            val description = it.getAsString("description")

            crunchyrollPlatform.animeInfoCache[CountryCodeIdKeyCache(countryCode, it.getAsString("id")!!)] =
                CrunchyrollPlatform.CrunchyrollAnimeContent(image = image, banner = banner, description = description)
        }

        val episodeIds = ids.parallelStream().map { seriesId ->
            runBlocking { CrunchyrollWrapper.getSeasons(countryCode.locale, accessToken, cms, seriesId) }
                .filter { jsonObject ->
                    jsonObject.getAsJsonArray("subtitle_locales").map { it.asString }.contains(countryCode.locale)
                }
                .map { jsonObject -> jsonObject.getAsString("id")!! }
                .flatMap { id ->
                    runBlocking {
                        CrunchyrollWrapper.getEpisodes(
                            countryCode.locale,
                            accessToken,
                            cms,
                            id
                        )
                    }
                }
                .map { jsonObject -> jsonObject.getAsString("id")!! }
        }.toList().flatten().toSet()

        episodeIds.chunked(25).parallelStream().forEach { episodeIdsChunked ->
            val `object` = runBlocking {
                CrunchyrollWrapper.getObject(
                    countryCode.locale,
                    accessToken,
                    cms,
                    *episodeIdsChunked.toTypedArray()
                )
            }

            `object`.forEach { episodeJson ->
                try {
                    episodes.add(
                        crunchyrollPlatform.convertEpisode(
                            countryCode,
                            episodeJson,
                            false,
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
}