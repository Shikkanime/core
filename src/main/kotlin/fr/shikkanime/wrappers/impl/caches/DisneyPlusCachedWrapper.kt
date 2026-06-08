package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper

object DisneyPlusCachedWrapper : AbstractDisneyPlusWrapper() {
    override suspend fun getLatestShowIds() = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getLatestShowIds",
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {},
        key = StringUtils.EMPTY_STRING
    ) { DisneyPlusWrapper.getLatestShowIds() }

    override suspend fun getShow(locale: String, id: String) = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = locale to id
    ) { DisneyPlusWrapper.getShow(it.first, it.second) }

    override suspend fun getEpisodesByShowId(
        locale: String,
        showId: String,
        checkAudioLocales: Boolean,
    ) = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = Triple(locale, showId, checkAudioLocales)
    ) { DisneyPlusWrapper.getEpisodesByShowId(it.first, it.second, it.third) }

    override suspend fun getMetadataByEpisodeId(episodeId: String) = MapCache.getOrComputeAsync(
        "DisneyPlusCachedWrapper.getMetadataByEpisodeId",
        typeToken = object : TypeToken<MapCacheValue<Metadata>>() {},
        key = episodeId
    ) { DisneyPlusWrapper.getMetadataByEpisodeId(it) }
}