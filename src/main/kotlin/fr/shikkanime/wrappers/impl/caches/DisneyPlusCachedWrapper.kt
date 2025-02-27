package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration

object DisneyPlusCachedWrapper : AbstractDisneyPlusWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    override suspend fun getShow(id: String) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getShow",
        duration = defaultCacheDuration,
        key = id
    ) { runBlocking { DisneyPlusWrapper.getShow(it) } }

    override suspend fun getEpisodesByShowId(
        locale: String,
        showId: String
    ) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getEpisodesByShowId",
        duration = defaultCacheDuration,
        key = locale to showId
    ) { runBlocking { DisneyPlusWrapper.getEpisodesByShowId(it.first, it.second) } }

    override suspend fun getShowIdByEpisodeId(episodeId: String) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getShowIdByEpisodeId",
        duration = defaultCacheDuration,
        key = episodeId
    ) { runBlocking { DisneyPlusWrapper.getShowIdByEpisodeId(it) } }

    override suspend fun getAudioLocales(resourceId: String) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getAudioLocales",
        duration = defaultCacheDuration,
        key = resourceId
    ) { runBlocking { DisneyPlusWrapper.getAudioLocales(it) } }
}