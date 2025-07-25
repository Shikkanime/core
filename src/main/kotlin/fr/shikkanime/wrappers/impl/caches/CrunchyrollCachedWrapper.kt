package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.ZonedDateTime

object CrunchyrollCachedWrapper : AbstractCrunchyrollWrapper() {
    private val objectCache = MapCache<Pair<String, String>, BrowseObject>("CrunchyrollCachedWrapper.objectCache") {
        runBlocking { CrunchyrollWrapper.getObjects(it.first, it.second).first() }
    }

    private val seriesCache = MapCache<Pair<String, String>, Series>("CrunchyrollCachedWrapper.seriesCache") {
        runBlocking { CrunchyrollWrapper.getSeries(it.first, it.second) }
            .also { series -> objectCache.setIfNotExists(it.first to it.second, series.convertToBrowseObject()) }
    }

    private val episodeCache = MapCache<Pair<String, String>, Episode>("CrunchyrollCachedWrapper.episodeCache") {
        runBlocking { CrunchyrollWrapper.getEpisode(it.first, it.second) }
            .also { episode -> objectCache.setIfNotExists(it.first to it.second, episode.convertToBrowseObject()) }
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
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getSeason",
        key = locale to id
    ) { runBlocking { CrunchyrollWrapper.getSeason(it.first, it.second) } }

    override suspend fun getSeasonsBySeriesId(
        locale: String,
        id: String
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getSeasonsBySeriesId",
        key = locale to id
    ) { runBlocking { CrunchyrollWrapper.getSeasonsBySeriesId(it.first, it.second) } }

    override suspend fun getEpisodesBySeasonId(
        locale: String,
        id: String
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getEpisodesBySeasonId",
        key = locale to id
    ) {
        runBlocking { CrunchyrollWrapper.getEpisodesBySeasonId(it.first, it.second) }
            .also { episodes -> episodes.forEach { episode ->
                episodeCache.setIfNotExists(it.first to episode.id, episode)
                objectCache.setIfNotExists(it.first to episode.id, episode.convertToBrowseObject())
            } }
    }

    override suspend fun getEpisode(
        locale: String,
        id: String
    ) = episodeCache[locale to id] ?: throw Exception("Failed to get episode")

    override suspend fun getEpisodeDiscoverByType(
        locale: String,
        type: String,
        id: String
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getEpisodeByType",
        key = Triple(locale, type, id)
    ) {
        runBlocking { CrunchyrollWrapper.getEpisodeDiscoverByType(it.first, it.second, it.third) }
            .also { episode -> objectCache.setIfNotExists(it.first to episode.id, episode) }
    }

    @JvmStatic
    suspend fun getPreviousEpisode(locale: String, id: String) = getEpisodeDiscoverByType(locale, "previous_episode", id)

    @JvmStatic
    suspend fun getUpNext(locale: String, id: String) = getEpisodeDiscoverByType(locale, "up_next", id)

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
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getEpisodesBySeriesId",
        key = Triple(locale, id, original)
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

    private val seriesRegex = "/series/([A-Z0-9]{9})/".toRegex()
    private val episodeRegex = "/watch/([A-Z0-9]{9})".toRegex()

    fun getSimulcastCalendarWithDates(countryCode: CountryCode, dates: Set<LocalDate>): List<BrowseObject> {
        val startOfWeekDates = dates.map { it.atStartOfWeek() }.distinct()
        val releaseDateTimes = mutableSetOf<ZonedDateTime>()
        val seriesIds = mutableSetOf<String>()
        val episodeIds = mutableSetOf<String>()

        startOfWeekDates.parallelStream().forEach { date ->
            val document = HttpRequest.retry(5) {
                Jsoup.parse(
                    httpRequest.get("$baseUrl${countryCode.name.lowercase()}/simulcastcalendar?filter=premium&date=$date").apply {
                        require(status == HttpStatusCode.OK)
                    }.bodyAsText())
            }

            document.select("article.release").forEach { element ->
                val releaseDateTime = ZonedDateTime.parse(element.select("time").attr("datetime")).withUTC()
                releaseDateTimes.add(releaseDateTime)

                if (StringUtils.DASH_STRING in element.attr("data-episode-num")) {
                    seriesIds.add(seriesRegex.find(element.select("a[href~=${seriesRegex.pattern}]").attr("href"))!!.groupValues[1])
                } else {
                    episodeIds.add(episodeRegex.find(element.select("a[href~=${episodeRegex.pattern}]").attr("href"))!!.groupValues[1])
                }
            }
        }

        episodeIds.addAll(
            seriesIds.asSequence()
                .flatMap { seriesId -> HttpRequest.retry(3) { getSeasonsBySeriesId(countryCode.locale, seriesId) } }
                .flatMap { season -> HttpRequest.retry(3) { getEpisodesBySeasonId(countryCode.locale, season.id) } }
                .flatMap { episode -> (listOf(episode.id) + episode.getVariants(null)) }
                .distinct()
        )

        return episodeIds.chunked(CRUNCHYROLL_CHUNK).parallelStream()
            .flatMap { chunk -> HttpRequest.retry(3) { getObjects(countryCode.locale, *chunk.toTypedArray()) }.stream() }
            .filter { it.episodeMetadata!!.premiumAvailableDate.withUTC() in releaseDateTimes }
            .toList()
            .apply {
                map { it.episodeMetadata!!.seriesId }.distinct().parallelStream().forEach { HttpRequest.retry(3) { getObjects(countryCode.locale, it) } }
                map { it.episodeMetadata!!.seasonId }.distinct().parallelStream().forEach { HttpRequest.retry(3) { getSeason(countryCode.locale, it) } }
            }
    }
}