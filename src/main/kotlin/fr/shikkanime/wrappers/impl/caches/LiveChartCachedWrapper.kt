package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractLiveChartWrapper
import fr.shikkanime.wrappers.impl.LiveChartWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

object LiveChartCachedWrapper : AbstractLiveChartWrapper() {
    override suspend fun getAnimeIdsFromDate(date: LocalDate) = MapCache.getOrCompute(
        "LiveChartCachedWrapper.getAnimeIdsFromDate",
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        key = date
    ) { runBlocking { LiveChartWrapper.getAnimeIdsFromDate(it) } }

    override suspend fun getStreamsByAnimeId(animeId: String) = MapCache.getOrCompute(
        "LiveChartCachedWrapper.getStreamsByAnimeId",
        typeToken = object : TypeToken<MapCacheValue<HashMap<Platform, Set<String>>>>() {},
        key = animeId
    ) { runBlocking { LiveChartWrapper.getStreamsByAnimeId(it) } }
}