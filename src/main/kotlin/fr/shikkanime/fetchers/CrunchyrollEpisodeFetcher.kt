package fr.shikkanime.fetchers

import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper.BrowseObject
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper.CrunchyrollResponse
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import jakarta.inject.Inject
import java.io.File
import java.time.ZonedDateTime

class CrunchyrollEpisodeFetcher {
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @Inject private lateinit var animeCacheService: AnimeCacheService

    /**
     * Retrieves a set of `BrowseObject` items by processing and aggregating cached data, API content,
     * and localized episodes. The method handles multiple countries defined in the Crunchyroll platform,
     * populates additional metadata, and fetches upcoming episodes based on the provided date-time and file input.
     *
     * @param crunchyrollPlatform The Crunchyroll platform configuration used to retrieve API content and country-specific data.
     * @param zonedDateTime The date-time used as a reference point for fetching episode data and determining time-based filters.
     * @param file An optional file containing cached data for episodes. If the file exists, its content is deserialized and used.
     * @return A set of `BrowseObject` items, aggregated from cached data, API responses, and additional preloaded or fetched content.
     */
    suspend fun fetch(crunchyrollPlatform: CrunchyrollPlatform, zonedDateTime: ZonedDateTime, file: File?): Set<BrowseObject> {
        val fileExists = file?.exists() ?: false
        val result =
            if (fileExists) ObjectParser.fromJson<CrunchyrollResponse<BrowseObject>>(file.inputStream()).data.toMutableSet()
            else mutableSetOf()

        crunchyrollPlatform.configuration?.availableCountries?.forEach { countryCode ->
            val countryEpisodesBrowse = result.ifEmpty { crunchyrollPlatform.getApiContent(countryCode, zonedDateTime) }
            result.addAll(countryEpisodesBrowse)

            if (configCacheService.getValueAsBoolean(ConfigPropertyKey.CRUNCHYROLL_PRELOAD_DATAS, true))
                result.addAll(preloadVariantsAndSeries(countryCode, countryEpisodesBrowse))

            if (configCacheService.getValueAsBoolean(ConfigPropertyKey.CRUNCHYROLL_PREDICT_NEXT_EPISODES, true))
                result.addAll(fetchUpcomingEpisodes(countryCode, crunchyrollPlatform, zonedDateTime, fileExists, countryEpisodesBrowse))
        }

        return result
    }

    /**
     * Preloads variant and series data for the given collection of browse objects based on the provided country code.
     *
     * This method processes the given collection of episode-related objects to derive applicable variant and series IDs,
     * applies locale-based filtering, and retrieves additional data from a cached source. It returns a set of browse objects
     * representing the preloaded data.
     *
     * @param countryCode The country code used to determine valid locales for filtering episode variants and series.
     * @param countryEpisodesBrowse A collection of browse objects representing episodes, which may include metadata for series and variants.
     * @return A set of browse objects containing the preloaded variants and series data.
     */
    suspend fun preloadVariantsAndSeries(
        countryCode: CountryCode,
        countryEpisodesBrowse: Collection<BrowseObject>
    ): Set<BrowseObject> {
        val currentEpisodeIds = countryEpisodesBrowse.map(BrowseObject::id).toSet()
        val variantIds = countryEpisodesBrowse.flatMap { browseObject ->
            val metadata = browseObject.episodeMetadata ?: return@flatMap emptyList<String>()
            val versions = metadata.versions ?: return@flatMap emptyList<String>()

            val allEpisodeAudioLocales = versions.map(AbstractCrunchyrollWrapper.Version::audioLocale).toSet() + setOfNotNull(metadata.audioLocale)
            val allowedAudioLocales = LocaleUtils.getAllowedLocales(countryCode, allEpisodeAudioLocales)

            versions.filter { it.audioLocale in allowedAudioLocales && it.guid !in currentEpisodeIds }
                .mapNotNull { it.guid.takeIfNotBlank() }
        }
        val seriesIds = countryEpisodesBrowse.mapNotNull { it.episodeMetadata?.seriesId?.takeIfNotBlank() }.toSet()

        val preloadIds = (variantIds + seriesIds).distinct()
        val preloadObjects =
            (if (preloadIds.isNotEmpty()) runCatching { CrunchyrollCachedWrapper.getObjects(countryCode.locale, *preloadIds.toTypedArray()) }.getOrNull() ?: emptyList()
            else emptyList())
                .associateBy(BrowseObject::id)

        return variantIds.mapNotNull { preloadObjects[it] }.toSet()
    }

    /**
     * Fetches a list of upcoming episodes based on the provided parameters, combining predicted next episodes
     * and newly available simulcast episodes.
     *
     * @param countryCode The country code used to filter episodes and determine locale-specific configurations.
     * @param crunchyrollPlatform The Crunchyroll platform identifier used to filter episodes for a specific platform.
     * @param zonedDateTime The reference date-time used to determine the time range for upcoming episodes.
     * @param fileExists A flag indicating whether a specific file exists, which affects whether simulcast episodes are fetched.
     * @param countryEpisodesBrowse A collection of browse objects representing episodes that are available for a given country.
     * @return A list of browse objects representing the upcoming episodes, uniquely identified and aggregated
     *         from both predicted releases and simulcast data.
     */
    suspend fun fetchUpcomingEpisodes(
        countryCode: CountryCode,
        crunchyrollPlatform: CrunchyrollPlatform,
        zonedDateTime: ZonedDateTime,
        fileExists: Boolean,
        countryEpisodesBrowse: Collection<BrowseObject>,
    ): Set<BrowseObject> {
        val weeksToPredict = configCacheService.getValueAsLong(ConfigPropertyKey.PREDICT_FUTURE_EPISODES_WEEKS, 1)
        val checkSeriesSimulcast = configCacheService.getValueAsBoolean(ConfigPropertyKey.CRUNCHYROLL_CHECK_SERIES_SIMULCAST, true)

        val previousWeek = zonedDateTime.minusWeeks(weeksToPredict)
        val previousWeekLocalDate = previousWeek.toLocalDate()
        val atStartOfDayPreviousWeek = previousWeekLocalDate.atStartOfDay(Constant.utcZoneId)
        val atEndOfDayPreviousWeek = previousWeekLocalDate.atEndOfTheDay(Constant.utcZoneId)

        val predictedNextEpisodes = episodeVariantCacheService.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
            countryCode,
            crunchyrollPlatform.getPlatform(),
            atStartOfDayPreviousWeek,
            atEndOfDayPreviousWeek
        ).filter { (_, releaseDateTime) -> previousWeek.isBetween(releaseDateTime..releaseDateTime.plusMinutes(30)) }
            .mapNotNull { (identifier, _) -> StringUtils.getVideoOldIdOrId(identifier)?.let { CrunchyrollWrapper.retrieveNextEpisode(countryCode.locale, it) } }

        val simulcastEpisodes =
            if (!fileExists && checkSeriesSimulcast) fetchNewSimulcastEpisodes(countryCode, crunchyrollPlatform, countryEpisodesBrowse)
            else emptyList()

        return (predictedNextEpisodes + simulcastEpisodes).distinctBy(BrowseObject::id).toSet()
    }

    /**
     * Fetches new simulcast episodes for a specific country and platform, excluding episodes that are
     * already present in the given collection or associated with current simulcasts.
     *
     * @param countryCode The country code used to determine the locale for fetching simulcast episodes.
     * @param crunchyrollPlatform The platform configuration for Crunchyroll used in identifying platform-specific episodes.
     * @param countryEpisodesBrowse A collection of browse objects representing episodes already fetched or processed.
     * @return A list of browse objects representing new simulcast episodes that are not already included
     *         in the provided collection or marked in the current simulcast cache.
     */
    private suspend fun fetchNewSimulcastEpisodes(
        countryCode: CountryCode,
        crunchyrollPlatform: CrunchyrollPlatform,
        countryEpisodesBrowse: Collection<BrowseObject>,
    ): List<BrowseObject> {
        val latestCrunchyrollSimulcasts = CrunchyrollCachedWrapper.getSimulcasts(countryCode.locale).firstOrNull() ?: return emptyList()
        val fetchApiSize = configCacheService.getValueAsInt(ConfigPropertyKey.CRUNCHYROLL_FETCH_API_SIZE, 25)
        val currentAnimes = animeCacheService.findAllByCurrentSimulcastAndLastSimulcast()

        return CrunchyrollWrapper.getBrowse(countryCode.locale, type = AbstractCrunchyrollWrapper.MediaType.SERIES, size = fetchApiSize, simulcast = latestCrunchyrollSimulcasts.id)
            .filterNot { series ->
                countryEpisodesBrowse.any { it.episodeMetadata?.id == series.id }
                        || currentAnimes.any { anime -> anime.platformIds?.any { it.platform.id == crunchyrollPlatform.getPlatform().name && it.platformId == series.id } == true }
            }
            .flatMap { series -> CrunchyrollCachedWrapper.getEpisodesBySeriesId(countryCode.locale, series.id).toList() }
    }
}