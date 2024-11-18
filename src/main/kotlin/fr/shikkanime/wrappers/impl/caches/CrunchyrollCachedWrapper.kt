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

    private val seriesCache = MapCache<Pair<String, String>, Series>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getSeries(it.first, it.second) }
    }

    private val seasonCache = MapCache<Pair<String, String>, Season>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getSeason(it.first, it.second) }
    }

    private val seasonsBySeriesIdCache = MapCache<Pair<String, String>, Array<Season>>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getSeasonsBySeriesId(it.first, it.second) }
            .apply { forEach { season -> seasonCache.setIfNotExists(it.first to season.id, season) } }
    }

    private val objectCache = MapCache<Pair<String, String>, BrowseObject>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getObjects(it.first, it.second).first() }
    }

    private val episodeCache = MapCache<Pair<String, String>, Episode>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getEpisode(it.first, it.second) }
            .apply { objectCache.setIfNotExists(it.first to it.second, this.convertToBrowseObject()) }
    }

    private val episodesBySeasonIdCache = MapCache<Pair<String, String>, Array<Episode>>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getEpisodesBySeasonId(it.first, it.second) }
            .apply { forEach { episode ->
                episodeCache.setIfNotExists(it.first to episode.id!!, episode)
                objectCache.setIfNotExists(it.first to episode.id, episode.convertToBrowseObject())
            } }
    }

    private val episodeByTypeCache = MapCache<Triple<String, String, String>, BrowseObject>(defaultCacheDuration) {
        runBlocking { CrunchyrollWrapper.getEpisodeByType(it.first, it.second, it.third) }
            .apply { objectCache.setIfNotExists(it.first to id, this) }
    }
    
    private val episodesBySeriesIdCache = MapCache<Triple<String, String, Boolean?>, Array<BrowseObject>>(defaultCacheDuration) { triple ->
        runBlocking {
            val browseObjects = mutableListOf<BrowseObject>()

            val variantObjects = getSeasonsBySeriesId(triple.first, triple.second)
                .flatMap { season ->
                    getEpisodesBySeasonId(triple.first, season.id)
                        .onEach { episode -> browseObjects.add(episode.convertToBrowseObject()) }
                        .flatMap { it.getVariants(triple.third) }
                }
                .subtract(browseObjects.map { it.id }.toSet())
                .chunked(CRUNCHYROLL_CHUNK)
                .flatMap { chunk -> HttpRequest.retry(3) { getObjects(triple.first, *chunk.toTypedArray()).toList() } }

            (browseObjects + variantObjects).toTypedArray()
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

    override suspend fun getSeasonsBySeriesId(
        locale: String,
        id: String
    ) = seasonsBySeriesIdCache[locale to id] ?: throw Exception("Failed to get seasons with series id")

    override suspend fun getSeason(
        locale: String,
        id: String
    ) = seasonCache[locale to id] ?: throw Exception("Failed to get season")

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
    ): Array<BrowseObject> {
        val predicate: (String) -> Boolean = { objectCache.containsKey(locale to it) }
        val alreadyCached = ids.filter(predicate)
        val notCached = ids.filterNot(predicate)
        val objects = alreadyCached.mapNotNull { objectCache[locale to it] }.toMutableList()

        if (notCached.isNotEmpty()) {
            val newObjects = CrunchyrollWrapper.getObjects(locale, *notCached.toTypedArray())
            newObjects.forEach { objectCache.setIfNotExists(locale to it.id, it) }
            objects.addAll(newObjects)
        }

        return objects.toTypedArray()
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
        val episodeIds = mutableSetOf<String>()

        startOfWeekDates.forEach { date ->
            val response = HttpRequest.retry(3) {
                val response = httpRequest.get("$baseUrl${countryCode.name.lowercase()}/simulcastcalendar?filter=premium&date=$date")
                require(response.status == HttpStatusCode.OK)
                response
            }

            val document = Jsoup.parse(response.bodyAsText())

            document.select("article.release").forEach { element ->
                val isMultipleRelease = element.attr("data-episode-num").contains("-")
                val releaseDateTime = ZonedDateTime.parse(element.select("time").attr("datetime")).withUTC()
                val seriesId = seriesRegex.find(element.select("a").first { seriesRegex.containsMatchIn(it.attr("href")) }.attr("href"))!!.groupValues[1]

                if (isMultipleRelease) {
                    getEpisodesBySeriesId(countryCode.locale, seriesId).filter { it.episodeMetadata!!.premiumAvailableDate.withUTC() == releaseDateTime }
                        .forEach { episodeIds.add(it.id) }
                } else {
                    episodeIds.add(episodeRegex.find(element.select("a").first { episodeRegex.containsMatchIn(it.attr("href")) }.attr("href"))!!.groupValues[1])
                }
            }
        }

        return episodeIds.chunked(CRUNCHYROLL_CHUNK).flatMap { chunk -> HttpRequest.retry(3) { getObjects(countryCode.locale, *chunk.toTypedArray()).toList() } }
    }
}