package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.atStartOfWeek
import fr.shikkanime.utils.withUTC
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

object CrunchyrollCachedWrapper : AbstractCrunchyrollWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    private val seriesCache = MapCache<Pair<String, String>, Series>(
        "CrunchyrollCachedWrapper.seriesCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getSeries(it.first, it.second) }
    }

    private val seasonCache = MapCache<Pair<String, String>, Season>(
        "CrunchyrollCachedWrapper.seasonCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getSeason(it.first, it.second) }
    }

    private val seasonsBySeriesIdCache = MapCache<Pair<String, String>, List<Season>>(
        "CrunchyrollCachedWrapper.seasonsBySeriesIdCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getSeasonsBySeriesId(it.first, it.second) }
    }

    private val objectCache = MapCache<Pair<String, String>, BrowseObject>(
        "CrunchyrollCachedWrapper.objectCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getObjects(it.first, it.second).first() }
    }

    private val episodeCache = MapCache<Pair<String, String>, Episode>(
        "CrunchyrollCachedWrapper.episodeCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getEpisode(it.first, it.second) }
            .also { episode -> objectCache.setIfNotExists(it.first to it.second, episode.convertToBrowseObject()) }
    }

    private val episodesBySeasonIdCache = MapCache<Pair<String, String>, List<Episode>>(
        "CrunchyrollCachedWrapper.episodesBySeasonIdCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getEpisodesBySeasonId(it.first, it.second) }
            .also { episodes -> episodes.forEach { episode ->
                episodeCache.setIfNotExists(it.first to episode.id!!, episode)
                objectCache.setIfNotExists(it.first to episode.id, episode.convertToBrowseObject())
            } }
    }

    private val episodeByTypeCache = MapCache<Triple<String, String, String>, BrowseObject>(
        "CrunchyrollCachedWrapper.episodeByTypeCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { CrunchyrollWrapper.getEpisodeByType(it.first, it.second, it.third) }
            .also { episode -> objectCache.setIfNotExists(it.first to episode.id, episode) }
    }
    
    private val episodesBySeriesIdCache = MapCache<Triple<String, String, Boolean?>, List<BrowseObject>>(
        "CrunchyrollCachedWrapper.episodesBySeriesIdCache",
        duration = defaultCacheDuration
    ) { triple ->
        runBlocking {
            val browseObjects = mutableListOf<BrowseObject>()

            val variantObjects = getSeasonsBySeriesId(triple.first, triple.second)
                .flatMap { season ->
                    getEpisodesBySeasonId(triple.first, season.id)
                        .onEach { episode -> browseObjects.add(episode.convertToBrowseObject()) }
                        .flatMap { it.getVariants(triple.third) }
                }
                .subtract(browseObjects.map { it.id })
                .chunked(CRUNCHYROLL_CHUNK)
                .flatMap { chunk -> HttpRequest.retry(3) { getObjects(triple.first, *chunk.toTypedArray()) } }

            browseObjects + variantObjects
        }
    }

    override suspend fun getBrowse(
        locale: String,
        sortBy: SortType,
        type: MediaType,
        size: Int,
        start: Int,
        simulcast: String?
    ) = CrunchyrollWrapper.getBrowse(locale, sortBy, type, size, start, simulcast)

    override suspend fun getSeries(
        locale: String,
        id: String
    ) = seriesCache[locale to id] ?: throw Exception("Failed to get series")

    override suspend fun getSeason(
        locale: String,
        id: String
    ) = seasonCache[locale to id] ?: throw Exception("Failed to get season")

    override suspend fun getSeasonsBySeriesId(
        locale: String,
        id: String
    ) = seasonsBySeriesIdCache[locale to id] ?: throw Exception("Failed to get seasons with series id")

    override suspend fun getEpisodesBySeasonId(
        locale: String,
        id: String
    ) = episodesBySeasonIdCache[locale to id] ?: throw Exception("Failed to get episodes by season id")

    override suspend fun getEpisode(
        locale: String,
        id: String
    ) = episodeCache[locale to id] ?: throw Exception("Failed to get episode")

    override suspend fun getEpisodeByType(
        locale: String,
        type: String,
        id: String
    ) = episodeByTypeCache[Triple(locale, type, id)] ?: throw Exception("Failed to get episode by type")

    @JvmStatic
    suspend fun getPreviousEpisode(locale: String, id: String) = getEpisodeByType(locale, "previous_episode", id)

    @JvmStatic
    suspend fun getUpNext(locale: String, id: String) = getEpisodeByType(locale, "up_next", id)

    override suspend fun getObjects(
        locale: String,
        vararg ids: String
    ): List<BrowseObject> {
        val predicate: (String) -> Boolean = { objectCache.containsKey(locale to it) }
        val alreadyCached = ids.filter(predicate)
        val notCached = ids.filterNot(predicate)
        val objects = alreadyCached.mapNotNull { objectCache[locale to it] }.toMutableList()

        if (notCached.isNotEmpty()) {
            val newObjects = CrunchyrollWrapper.getObjects(locale, *notCached.toTypedArray())
            newObjects.forEach { objectCache.setIfNotExists(locale to it.id, it) }
            objects.addAll(newObjects)
        }

        return objects
    }

    override suspend fun getEpisodesBySeriesId(
        locale: String,
        id: String,
        original: Boolean?
    ) = episodesBySeriesIdCache[Triple(locale, id, original)] ?: throw Exception("Failed to get episodes by series id")

    private val seriesRegex = "/series/([A-Z0-9]{9})/".toRegex()
    private val episodeRegex = "/watch/([A-Z0-9]{9})".toRegex()

    suspend fun getSimulcastCalendarWithDates(countryCode: CountryCode, dates: Set<LocalDate>): List<BrowseObject> {
        val startOfWeekDates = dates.map { it.atStartOfWeek() }.distinct()
        val releaseDateTimes = mutableSetOf<ZonedDateTime>()
        val seriesIds = mutableSetOf<String>()
        val episodeIds = mutableSetOf<String>()

        startOfWeekDates.forEach { date ->
            val response = HttpRequest.retry(3) {
                httpRequest.get("$baseUrl${countryCode.name.lowercase()}/simulcastcalendar?filter=premium&date=$date").apply {
                    require(status == HttpStatusCode.OK)
                }
            }

            val document = Jsoup.parse(response.bodyAsText())
            document.select("article.release").forEach { element ->
                val releaseDateTime = ZonedDateTime.parse(element.select("time").attr("datetime")).withUTC()
                releaseDateTimes.add(releaseDateTime)

                if (element.attr("data-episode-num").contains("-")) {
                    seriesIds.add(seriesRegex.find(element.select("a[href~=${seriesRegex.pattern}]").attr("href"))!!.groupValues[1])
                } else {
                    episodeIds.add(episodeRegex.find(element.select("a[href~=${episodeRegex.pattern}]").attr("href"))!!.groupValues[1])
                }
            }
        }

        episodeIds.addAll(
            seriesIds.parallelStream()
                .flatMap { seriesId -> HttpRequest.retry(3) { getSeasonsBySeriesId(countryCode.locale, seriesId) }.stream() }
                .flatMap { season -> HttpRequest.retry(3) { getEpisodesBySeasonId(countryCode.locale, season.id) }.stream() }
                .flatMap { episode -> (listOf(episode.id!!) + episode.getVariants(null)).stream() }
                .distinct()
                .toList()
        )

        return episodeIds.chunked(CRUNCHYROLL_CHUNK).parallelStream()
            .flatMap { chunk -> HttpRequest.retry(3) { getObjects(countryCode.locale, *chunk.toTypedArray()) }.stream() }
            .filter { it.episodeMetadata!!.premiumAvailableDate.withUTC() in releaseDateTimes }
            .toList()
            .apply {
                map { it.episodeMetadata!!.seriesId }.distinct().parallelStream().forEach { HttpRequest.retry(3) { getSeries(countryCode.locale, it) } }
                map { it.episodeMetadata!!.seasonId }.distinct().parallelStream().forEach { HttpRequest.retry(3) { getSeason(countryCode.locale, it) } }
            }
    }
}