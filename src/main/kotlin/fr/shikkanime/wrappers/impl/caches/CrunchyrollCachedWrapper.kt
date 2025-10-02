package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking

object CrunchyrollCachedWrapper : AbstractCrunchyrollWrapper() {
    private val objectCache = MapCache<Pair<String, String>, BrowseObject>(
        "CrunchyrollCachedWrapper.objectCache",
        typeToken = object : TypeToken<MapCacheValue<BrowseObject>>() {}
    ) { runBlocking { CrunchyrollWrapper.getObjects(it.first, it.second).first() } }

    private val seriesCache = MapCache<Pair<String, String>, Series>(
        "CrunchyrollCachedWrapper.seriesCache",
        typeToken = object : TypeToken<MapCacheValue<Series>>() {}
    ) {
        runBlocking { CrunchyrollWrapper.getSeries(it.first, it.second) }
            .also { series -> objectCache.putIfNotExists(it.first to it.second, series.convertToBrowseObject()) }
    }

    private val episodeCache = MapCache<Pair<String, String>, Episode>(
        "CrunchyrollCachedWrapper.episodeCache",
        typeToken = object : TypeToken<MapCacheValue<Episode>>() {}
    ) {
        runBlocking { CrunchyrollWrapper.getEpisode(it.first, it.second) }
            .also { episode -> objectCache.putIfNotExists(it.first to it.second, episode.convertToBrowseObject()) }
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
        typeToken = object : TypeToken<MapCacheValue<Season>>() {},
        key = locale to id
    ) { runBlocking { CrunchyrollWrapper.getSeason(it.first, it.second) } }

    override suspend fun getSeasonsBySeriesId(
        locale: String,
        id: String
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getSeasonsBySeriesId",
        typeToken = object : TypeToken<MapCacheValue<Array<Season>>>() {},
        key = locale to id
    ) { runBlocking { CrunchyrollWrapper.getSeasonsBySeriesId(it.first, it.second) } }

    override suspend fun getEpisodesBySeasonId(
        locale: String,
        id: String
    ) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getEpisodesBySeasonId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = locale to id
    ) {
        runBlocking { CrunchyrollWrapper.getEpisodesBySeasonId(it.first, it.second) }
            .also { episodes -> episodes.forEach { episode ->
                episodeCache.putIfNotExists(it.first to episode.id, episode)
                objectCache.putIfNotExists(it.first to episode.id, episode.convertToBrowseObject())
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
        typeToken = object : TypeToken<MapCacheValue<BrowseObject>>() {},
        key = Triple(locale, type, id)
    ) {
        runBlocking { CrunchyrollWrapper.getEpisodeDiscoverByType(it.first, it.second, it.third) }
            .also { episode -> objectCache.putIfNotExists(it.first to episode.id, episode) }
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
            newObjects.forEach { objectCache.putIfNotExists(locale to it.id, it) }
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
        typeToken = object : TypeToken<MapCacheValue<Array<BrowseObject>>>() {},
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
                .flatMap { chunk -> getObjects(triple.first, *chunk.toTypedArray()) }

            (browseObjects + variantObjects).toTypedArray()
        }
    }

    override suspend fun getSimulcasts(locale: String) = MapCache.getOrCompute(
        "CrunchyrollCachedWrapper.getSimulcasts",
        typeToken = object : TypeToken<MapCacheValue<Array<Simulcast>>>() {},
        key = locale
    ) { locale -> runBlocking { CrunchyrollWrapper.getSimulcasts(locale) } }
}