package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractLiveChart
import fr.shikkanime.wrappers.impl.LiveChartWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate

object LiveChartCachedWrapper : AbstractLiveChart() {
    private val defaultCacheDuration = Duration.ofDays(1)

    override suspend fun getAnimeIdsFromDate(date: LocalDate) = MapCache.getOrCompute(
        "LiveChartCachedWrapper.getAnimeIdsFromDate",
        duration = defaultCacheDuration,
        key = date
    ) { runBlocking { LiveChartWrapper.getAnimeIdsFromDate(it) } }

    override suspend fun getStreamsForAnime(animeId: String) = MapCache.getOrCompute(
        "LiveChartCachedWrapper.getStreamsForAnime",
        duration = defaultCacheDuration,
        key = animeId
    ) { runBlocking { LiveChartWrapper.getStreamsForAnime(it) } }
}