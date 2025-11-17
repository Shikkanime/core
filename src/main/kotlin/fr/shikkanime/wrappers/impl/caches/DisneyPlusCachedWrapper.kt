package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper

object DisneyPlusCachedWrapper : AbstractDisneyPlusWrapper() {
    override suspend fun getShow(id: String) = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = id
    ) { DisneyPlusWrapper.getShow(it) }

    override suspend fun getEpisodesByShowId(
        locale: String,
        showId: String,
        checkAudioLocales: Boolean,
    ) = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = Triple(locale, showId, checkAudioLocales)
    ) { DisneyPlusWrapper.getEpisodesByShowId(it.first, it.second, it.third) }

    override suspend fun getAudioLocales(resourceId: String) = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getAudioLocales",
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        key = resourceId
    ) { DisneyPlusWrapper.getAudioLocales(it) }
}