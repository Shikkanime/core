package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.*
import fr.shikkanime.platforms.configuration.CrunchyrollConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class CrunchyrollPlatform : AbstractPlatform<CrunchyrollConfiguration, CountryCode, List<AbstractCrunchyrollWrapper.BrowseObject>>() {
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    override fun getPlatform(): Platform = Platform.CRUN

    override fun getConfigurationClass() = CrunchyrollConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCode,
        zonedDateTime: ZonedDateTime
    ): List<AbstractCrunchyrollWrapper.BrowseObject> {
        return CrunchyrollWrapper.getBrowse(
            key.locale,
            size = configCacheService.getValueAsInt(ConfigPropertyKey.CRUNCHYROLL_FETCH_API_SIZE, 25)
        )
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = bypassFileContent?.takeIf { it.exists() }?.let {
                ObjectParser.fromJson(
                    ObjectParser.fromJson(it.readText()).getAsJsonArray("data"),
                    Array<AbstractCrunchyrollWrapper.BrowseObject>::class.java
                ).toList()
            } ?: getApiContent(countryCode, zonedDateTime).toMutableList()

            api.map { it.episodeMetadata!!.seriesId }.distinct().parallelStream().forEach {
                runCatching { HttpRequest.retry(3) { CrunchyrollCachedWrapper.getObjects(countryCode.locale, it) } }
            }

            api.map { it.episodeMetadata!!.seasonId }.distinct().parallelStream().forEach {
                runCatching { HttpRequest.retry(3) { CrunchyrollCachedWrapper.getSeason(countryCode.locale, it) } }
            }

            api.forEach { addToList(list, countryCode, it) }

            runBlocking { list.addAll(predictFutureEpisodes(countryCode, zonedDateTime, list)) }
        }

        return list
    }

    private fun addToList(
        list: MutableList<Episode>,
        countryCode: CountryCode,
        browseObject: AbstractCrunchyrollWrapper.BrowseObject
    ) {
        try {
            list.add(convertEpisode(countryCode, browseObject, needSimulcast = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_SIMULCAST, true)))
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
        val previousWeek = zonedDateTime.minusWeeks(1)
        val previousWeekLocalDate = previousWeek.toLocalDate()

        episodeVariantCacheService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
            countryCode,
            getPlatform(),
            previousWeekLocalDate.atStartOfDay(Constant.utcZoneId),
            previousWeekLocalDate.atEndOfTheDay(Constant.utcZoneId)
        ).filter { (_, releaseDateTime) -> previousWeek.isAfterOrEqual(releaseDateTime) }
            .forEach { (identifier, _) ->
                val crunchyrollId = StringUtils.getVideoOldIdOrId(identifier) ?: run {
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

    suspend fun getNextEpisode(countryCode: CountryCode, id: String): AbstractCrunchyrollWrapper.BrowseObject? {
        // Attempt to fetch the next episode directly
        runCatching { CrunchyrollWrapper.getUpNext(countryCode.locale, id) }.getOrNull()?.let { return it }

        logger.warning("Cannot fetch next episode for $id, trying alternative methods...")

        // Fetch the current episode and check for nextEpisodeId
        val episode = runCatching { CrunchyrollWrapper.getJvmStaticEpisode(countryCode.locale, id) }.getOrNull() ?: return null

        episode.nextEpisodeId?.let { nextEpisodeId ->
            runCatching { CrunchyrollWrapper.getJvmStaticObjects(countryCode.locale, nextEpisodeId) }.getOrNull()?.firstOrNull()
                ?.let { return it }
        }

        // Fetch episodes by season and find the next episode
        logger.warning("Next episode ID not found for $id, searching by season...")
        return runCatching { CrunchyrollWrapper.getJvmStaticEpisodesBySeasonId(countryCode.locale, episode.seasonId) }.getOrNull()
                ?.sortedBy { it.sequenceNumber }
                ?.firstOrNull { it.sequenceNumber > episode.sequenceNumber }
                ?.convertToBrowseObject()
    }

    fun convertEpisode(
        countryCode: CountryCode,
        browseObject: AbstractCrunchyrollWrapper.BrowseObject,
        needSimulcast: Boolean = true
    ): Episode {
        val seasonRegex = " Saison (\\d)".toRegex()
        var animeName = browseObject.episodeMetadata!!.seriesTitle
        var forcedSeason: Int? = null

        if (seasonRegex in animeName) {
            forcedSeason = seasonRegex.find(animeName)!!.groupValues[1].toIntOrNull()
            animeName = animeName.replace(seasonRegex, StringUtils.EMPTY_STRING)
        }

        if (isBlacklisted(animeName))
            throw AnimeException("\"$animeName\" is blacklisted")

        val isTeaser = browseObject.slugTitle?.contains("(teaser|pv|trailer)(?:-\\d)?".toRegex()) == true &&
                browseObject.episodeMetadata.premiumAvailableDate.withUTCString() == "1970-01-01T00:00:00Z"

        if (isTeaser)
            throw NotSimulcastedMediaException("Teaser is not simulcasted")

        val isDubbed = browseObject.episodeMetadata.audioLocale == countryCode.locale
        val subtitles = browseObject.episodeMetadata.subtitleLocales

        if (!isDubbed && (subtitles.isEmpty() || countryCode.locale !in subtitles))
            throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

        val crunchyrollAnimeContent = runBlocking { CrunchyrollCachedWrapper.getObjects(countryCode.locale, browseObject.episodeMetadata.seriesId).first() }
        val isConfigurationSimulcasted = containsAnimeSimulcastConfiguration(animeName)
        val season = runBlocking { CrunchyrollCachedWrapper.getSeason(countryCode.locale, browseObject.episodeMetadata.seasonId) }

        val (number, episodeType) = getNumberAndEpisodeType(browseObject.episodeMetadata, season)

        val isSimulcasted = crunchyrollAnimeContent.seriesMetadata!!.isSimulcast || isDubbed || episodeType == EpisodeType.FILM

        if (needSimulcast && !(isConfigurationSimulcasted || isSimulcasted))
            throw AnimeNotSimulcastedException("\"$animeName\" is not simulcasted")

        var original = true

        if (!browseObject.episodeMetadata.versions.isNullOrEmpty()) {
            val currentVersion = browseObject.episodeMetadata.versions.firstOrNull { it.guid == browseObject.id }
            original = currentVersion?.original != false
        }

        return Episode(
            countryCode = countryCode,
            animeId = browseObject.episodeMetadata.seriesId,
            anime = animeName,
            animeImage = crunchyrollAnimeContent.images!!.fullHDImage!!,
            animeBanner = crunchyrollAnimeContent.images.fullHDBanner!!,
            animeDescription = crunchyrollAnimeContent.description.normalize(),
            releaseDateTime = browseObject.episodeMetadata.premiumAvailableDate,
            episodeType = episodeType,
            seasonId = browseObject.episodeMetadata.seasonId,
            season = forcedSeason ?: (browseObject.episodeMetadata.seasonNumber ?: 1),
            number = number,
            duration = browseObject.episodeMetadata.durationMs / 1000,
            title = browseObject.title.normalize(),
            description = browseObject.description.normalize(),
            image = browseObject.images?.fullHDThumbnail ?: Constant.DEFAULT_IMAGE_PREVIEW,
            platform = getPlatform(),
            audioLocale = browseObject.episodeMetadata.audioLocale,
            id = browseObject.id,
            url = CrunchyrollWrapper.buildUrl(countryCode, browseObject.id, browseObject.slugTitle),
            uncensored = browseObject.episodeMetadata.matureBlocked,
            original = original
        )
    }

    private fun getNumberAndEpisodeType(
        episode: AbstractCrunchyrollWrapper.Episode,
        season: AbstractCrunchyrollWrapper.Season
    ): Pair<Int, EpisodeType> {
        var number = episode.number ?: -1
        val specialEpisodeRegex = "SP(\\d*)".toRegex()

        var episodeType = when {
            episode.seasonSlugTitle?.contains("movie", true) == true || season.keywords.any {
                it.contains(
                    "movie",
                    true
                )
            } -> EpisodeType.FILM
            number == -1 -> EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        specialEpisodeRegex.find(episode.numberString)?.let {
            episodeType = EpisodeType.SPECIAL
            number = it.groupValues[1].toIntOrNull() ?: -1
        }

        episode.identifier?.let { identifier ->
            "(.+)\\|(.+)\\|(.+)".toRegex().find(identifier)?.let {
                when (it.groupValues[2]) {
                    "OAD" -> {
                        episodeType = EpisodeType.SPECIAL
                    }
                }
            }
        }

        return number to episodeType
    }
}