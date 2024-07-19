package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeIdKeyCache
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.*
import fr.shikkanime.platforms.configuration.CrunchyrollConfiguration
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.withUTCString
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.util.logging.Level

class CrunchyrollPlatform :
    AbstractPlatform<CrunchyrollConfiguration, CountryCode, Array<CrunchyrollWrapper.BrowseObject>>() {
    data class CrunchyrollAnimeContent(
        val image: String,
        val banner: String = "",
        val description: String? = null,
        val simulcast: Boolean = false,
    )

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    private val identifiers = MapCache<CountryCode, String>(Duration.ofMinutes(30)) {
        return@MapCache runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
    }

    val simulcasts = MapCache<CountryCode, Set<String>>(Duration.ofHours(1)) {
        val simulcastSeries = mutableSetOf<String>()
        val accessToken = identifiers[it]!!
        val simulcasts = runBlocking { CrunchyrollWrapper.getSimulcasts(it.locale, accessToken) }.take(2)
            .map { simulcast -> simulcast.getAsString("id") }

        val series = simulcasts.flatMap { simulcastId ->
            runBlocking {
                CrunchyrollWrapper.getBrowse(
                    it.locale,
                    accessToken,
                    sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                    type = CrunchyrollWrapper.MediaType.SERIES,
                    100,
                    simulcast = simulcastId
                ).toList()
            }
        }.map { serie -> serie.title!!.lowercase() }.toSet()

        simulcastSeries.addAll(series)
        logger.info(simulcastSeries.joinToString(", "))
        return@MapCache simulcastSeries
    }

    private val animeInfoCache = MapCache<CountryCodeIdKeyCache, CrunchyrollAnimeContent?>(Duration.ofDays(1)) {
        try {
            val token = identifiers[it.countryCode]!!
            val series = runBlocking {
                CrunchyrollWrapper.getSeries(
                    it.countryCode.locale,
                    token,
                    it.id
                )
            }

            val image = series.images.posterTall.first().maxByOrNull { poster -> poster.width }?.source
            val banner = series.images.posterWide.first().maxByOrNull { poster -> poster.width }?.source

            if (image.isNullOrEmpty())
                throw Exception("Image is null or empty")

            if (banner.isNullOrEmpty())
                throw Exception("Banner is null or empty")

            return@MapCache CrunchyrollAnimeContent(image, banner, series.description, series.isSimulcast)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error on fetching anime info", e)
            return@MapCache null
        }
    }

    override fun getPlatform(): Platform = Platform.CRUN

    override fun getConfigurationClass() = CrunchyrollConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCode,
        zonedDateTime: ZonedDateTime
    ): Array<CrunchyrollWrapper.BrowseObject> {
        return CrunchyrollWrapper.getBrowse(
            key.locale,
            identifiers[key]!!,
            size = configCacheService.getValueAsInt(ConfigPropertyKey.CRUNCHYROLL_FETCH_API_SIZE, 25)
        )
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = if (bypassFileContent != null && bypassFileContent.exists()) {
                ObjectParser.fromJson(
                    ObjectParser.fromJson(bypassFileContent.readText()).getAsJsonArray("data"),
                    Array<CrunchyrollWrapper.BrowseObject>::class.java
                )
            } else {
                getApiContent(countryCode, zonedDateTime) // NOSONAR
            }.toMutableList()

            api.forEach {
                try {
                    list.add(convertEpisode(countryCode, it))
                } catch (_: EpisodeException) {
                    // Ignore
                } catch (_: AnimeException) {
                    // Ignore
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error on converting episode", e)
                }
            }

            runBlocking { list.addAll(predictFutureEpisodes(countryCode, zonedDateTime, list)) }
        }

        return list
    }

    override fun saveConfiguration() {
        super.saveConfiguration()
        simulcasts.resetWithNewDuration(Duration.ofMinutes(configuration!!.simulcastCheckDelayInMinutes))
    }

    private suspend fun predictFutureEpisodes(
        countryCode: CountryCode,
        zonedDateTime: ZonedDateTime,
        alreadyFetched: List<Episode>
    ): List<Episode> {
        val list = mutableListOf<Episode>()

        val lastWeek = zonedDateTime.minusWeeks(1)
        val lastWeekStartOfTheDay = lastWeek.withHour(0).withMinute(0).withSecond(0).withNano(0)

        episodeVariantService.findAllMappingUuidAndIdentifierByDateRange(
            countryCode,
            lastWeekStartOfTheDay,
            lastWeek.plusSeconds(1),
            getPlatform()
        ).forEach { pair ->
            episodeMappingCacheService.findNextEpisode(pair.first)?.let {
                logger.warning("Next episode already exists for ${pair.second}")
                return@forEach
            }

            val crunchyrollId = getCrunchyrollId(pair.second) ?: run {
                logger.warning("Crunchyroll ID not found in ${pair.second}")
                return@forEach
            }

            CrunchyrollWrapper.getEpisode(
                countryCode.locale,
                identifiers[countryCode]!!,
                crunchyrollId
            ).nextEpisodeId?.let { nextEpisodeId ->
                if (alreadyFetched.any { it.id == nextEpisodeId }) {
                    logger.warning("Episode $nextEpisodeId already fetched")
                    return@forEach
                }

                CrunchyrollWrapper.getObjects(countryCode.locale, identifiers[countryCode]!!, nextEpisodeId)
                    .forEach { browseObject ->
                        try {
                            list.add(convertEpisode(countryCode, browseObject))
                        } catch (_: EpisodeException) {
                            // Ignore
                        } catch (_: AnimeException) {
                            // Ignore
                        } catch (e: Exception) {
                            logger.log(Level.SEVERE, "Error on converting episode", e)
                        }
                    }
            } ?: logger.warning("Next episode ID not found in $crunchyrollId")
        }

        return list
    }

    fun getCrunchyrollId(identifier: String) =
        "[A-Z]{2}-CRUN-([A-Z0-9]{9})-[A-Z]{2}-[A-Z]{2}".toRegex().find(identifier)?.groupValues?.get(1)

    fun convertEpisode(
        countryCode: CountryCode,
        browseObject: CrunchyrollWrapper.BrowseObject,
        needSimulcast: Boolean = true
    ): Episode {
        val seasonRegex = " Saison (\\d)".toRegex()
        var animeName = browseObject.episodeMetadata!!.seriesTitle
        var forcedSeason: Int? = null

        if (animeName.contains(seasonRegex)) {
            forcedSeason = seasonRegex.find(animeName)!!.groupValues[1].toIntOrNull()
            animeName = animeName.replace(seasonRegex, "")
        }

        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase()))
            throw AnimeException("\"$animeName\" is blacklisted")

        val isTeaser = browseObject.slugTitle?.contains("teaser", true) == true &&
                browseObject.episodeMetadata.premiumAvailableDate.withUTCString() == "1970-01-01T00:00:00Z"

        if (isTeaser)
            throw EpisodeException("Episode is a teaser")

        val isDubbed = browseObject.episodeMetadata.audioLocale == countryCode.locale
        val subtitles = browseObject.episodeMetadata.subtitleLocales

        if (!isDubbed && (subtitles.isEmpty() || !subtitles.contains(countryCode.locale)))
            throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

        val checkCrunchyrollSimulcasts =
            configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_CRUNCHYROLL_SIMULCASTS, true)
        val isConfigurationSimulcast = configuration!!.simulcasts.any { it.name.lowercase() == animeName.lowercase() }

        if (needSimulcast && checkCrunchyrollSimulcasts && !(isConfigurationSimulcast || simulcasts[countryCode]!!.contains(
                animeName.lowercase()
            ))
        )
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        val crunchyrollAnimeContent =
            animeInfoCache[CountryCodeIdKeyCache(countryCode, browseObject.episodeMetadata.seriesId)] ?: throw AnimeException("Anime not found")

        if (needSimulcast && !checkCrunchyrollSimulcasts && !(isConfigurationSimulcast || crunchyrollAnimeContent.simulcast))
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        val (number, episodeType) = getNumberAndEpisodeType(browseObject.episodeMetadata)

        var original = true

        if (!browseObject.episodeMetadata.versions.isNullOrEmpty()) {
            val currentVersion = browseObject.episodeMetadata.versions.firstOrNull { it.guid == browseObject.id }
            original = currentVersion?.original ?: true
        }

        return Episode(
            countryCode = countryCode,
            anime = animeName,
            animeImage = crunchyrollAnimeContent.image,
            animeBanner = crunchyrollAnimeContent.banner,
            animeDescription = crunchyrollAnimeContent.description,
            releaseDateTime = browseObject.episodeMetadata.premiumAvailableDate,
            episodeType = episodeType,
            season = forcedSeason ?: (browseObject.episodeMetadata.seasonNumber ?: 1),
            number = number,
            duration = browseObject.episodeMetadata.durationMs / 1000,
            title = browseObject.title,
            description = browseObject.description?.replace('\n', ' ')?.takeIf { it.isNotBlank() },
            image = browseObject.images?.thumbnail?.firstOrNull()
                ?.maxByOrNull { it.width }?.source?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_IMAGE_PREVIEW,
            platform = getPlatform(),
            audioLocale = browseObject.episodeMetadata.audioLocale,
            id = browseObject.id,
            url = CrunchyrollWrapper.buildUrl(countryCode, browseObject.id, browseObject.slugTitle),
            uncensored = false,
            original = original
        )
    }

    fun getNumberAndEpisodeType(episode: CrunchyrollWrapper.Episode): Pair<Int, EpisodeType> {
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