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
import fr.shikkanime.utils.*
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

    val identifiers = MapCache<CountryCode, String>(Duration.ofMinutes(30)) {
        return@MapCache runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
    }

    private val animeInfoCache = MapCache<CountryCodeIdKeyCache, CrunchyrollAnimeContent?>(Duration.ofDays(1)) {
        try {
            val token = identifiers[it.countryCode]!!
            val series = HttpRequest.retry(3) { CrunchyrollWrapper.getSeries(it.countryCode.locale, token, it.id) }

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

            api.forEach { addToList(list, countryCode, it) }

            runBlocking { list.addAll(predictFutureEpisodes(countryCode, zonedDateTime, list)) }
        }

        return list
    }

    private fun addToList(
        list: MutableList<Episode>,
        countryCode: CountryCode,
        browseObject: CrunchyrollWrapper.BrowseObject
    ) {
        try {
            list.add(convertEpisode(countryCode, browseObject))
        } catch (_: EpisodeException) {
            // Ignore
        } catch (_: AnimeException) {
            // Ignore
        } catch (_: NotSimulcastedMediaException) {
            // Ignore
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error on converting episode", e)
        }
    }

    private suspend fun predictFutureEpisodes(
        countryCode: CountryCode,
        zonedDateTime: ZonedDateTime,
        alreadyFetched: List<Episode>
    ): List<Episode> {
        val list = mutableListOf<Episode>()
        val lastWeek = zonedDateTime.minusWeeks(1)

        episodeVariantService.findAllIdentifierByDateRangeWithoutNextEpisode(
            countryCode,
            lastWeek.toLocalDate().atStartOfDay(Constant.utcZoneId),
            lastWeek.plusSeconds(1).withUTC(),
            getPlatform()
        ).forEach { identifier ->
            val crunchyrollId = getCrunchyrollId(identifier) ?: run {
                logger.warning("Crunchyroll ID not found in $identifier")
                return@forEach
            }

            val nextEpisode = getNextEpisode(countryCode, crunchyrollId) ?: run {
                logger.warning("Next episode not found for $crunchyrollId")
                return@forEach
            }

            if (alreadyFetched.any { it.id == nextEpisode.id }) {
                logger.warning("Episode ${nextEpisode.id} already fetched")
                return@forEach
            }

            addToList(list, countryCode, nextEpisode)
        }

        return list
    }

    suspend fun getNextEpisode(countryCode: CountryCode, crunchyrollId: String): CrunchyrollWrapper.BrowseObject? {
        return try {
            CrunchyrollWrapper.getUpNext(countryCode.locale, identifiers[countryCode]!!, crunchyrollId)
        } catch (_: Exception) {
            logger.warning("Can not fetch up next episode for $crunchyrollId, trying to check with the episode...")
            val episode = CrunchyrollWrapper.getEpisode(countryCode.locale, identifiers[countryCode]!!, crunchyrollId)

            if (!episode.nextEpisodeId.isNullOrEmpty()) {
                return CrunchyrollWrapper.getObjects(
                    countryCode.locale,
                    identifiers[countryCode]!!,
                    episode.nextEpisodeId
                ).firstOrNull()
            }

            logger.warning("Next episode ID not found for $crunchyrollId, trying to find it by season...")
            val episodes = CrunchyrollWrapper.getEpisodesBySeasonId(countryCode.locale, identifiers[countryCode]!!, episode.seasonId)
            episodes.firstOrNull { it.premiumAvailableDate > episode.premiumAvailableDate }
                ?.let { CrunchyrollWrapper.getObjects(countryCode.locale, identifiers[countryCode]!!, it.id!!).firstOrNull() }
        }
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
            throw NotSimulcastedMediaException("Teaser is not simulcasted")

        val isDubbed = browseObject.episodeMetadata.audioLocale == countryCode.locale
        val subtitles = browseObject.episodeMetadata.subtitleLocales

        if (!isDubbed && (subtitles.isEmpty() || !subtitles.contains(countryCode.locale)))
            throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

        val crunchyrollAnimeContent =
            animeInfoCache[CountryCodeIdKeyCache(countryCode, browseObject.episodeMetadata.seriesId)]
                ?: throw AnimeException("Anime not found")
        val isConfigurationSimulcast = configuration!!.simulcasts.any { it.name.lowercase() == animeName.lowercase() }

        if (needSimulcast && !(isConfigurationSimulcast || crunchyrollAnimeContent.simulcast))
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        val (number, episodeType) = getNumberAndEpisodeType(browseObject.episodeMetadata)

        var original = true

        if (!browseObject.episodeMetadata.versions.isNullOrEmpty()) {
            val currentVersion = browseObject.episodeMetadata.versions.firstOrNull { it.guid == browseObject.id }
            original = currentVersion?.original != false
        }

        return Episode(
            countryCode = countryCode,
            animeId = browseObject.episodeMetadata.seriesId,
            anime = animeName,
            animeImage = crunchyrollAnimeContent.image,
            animeBanner = crunchyrollAnimeContent.banner,
            animeDescription = crunchyrollAnimeContent.description.normalize(),
            releaseDateTime = browseObject.episodeMetadata.premiumAvailableDate,
            episodeType = episodeType,
            season = forcedSeason ?: (browseObject.episodeMetadata.seasonNumber ?: 1),
            number = number,
            duration = browseObject.episodeMetadata.durationMs / 1000,
            title = browseObject.title.normalize(),
            description = browseObject.description.normalize(),
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

        return number to episodeType
    }
}