package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import kotlinx.coroutines.runBlocking

object DisneyPlusCachedWrapper : AbstractDisneyPlusWrapper() {
    override suspend fun getShow(id: String) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = id
    ) { runBlocking { DisneyPlusWrapper.getShow(it) } }

    override suspend fun getEpisodesByShowId(
        locale: String,
        showId: String,
        checkAudioLocales: Boolean,
    ) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = Triple(locale, showId, checkAudioLocales)
    ) { runBlocking { DisneyPlusWrapper.getEpisodesByShowId(it.first, it.second, it.third) } }

    override suspend fun getAudioLocales(resourceId: String) = MapCache.getOrCompute(
        "DisneyPlusCachedWrapper.getAudioLocales",
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        key = resourceId
    ) { runBlocking { DisneyPlusWrapper.getAudioLocales(it) } }
}