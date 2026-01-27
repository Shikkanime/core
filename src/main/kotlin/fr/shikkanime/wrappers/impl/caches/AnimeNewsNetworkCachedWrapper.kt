package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractAnimeNewsNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimeNewsNetworkWrapper

object AnimeNewsNetworkCachedWrapper : AbstractAnimeNewsNetworkWrapper() {
    override suspend fun getMediaById(id: Int) = MapCache.getOrComputeAsync(
        "AnimeNewsNetworkCachedWrapper.getMediaById",
        typeToken = object : TypeToken<MapCacheValue<Media>>() {},
        key = id
    ) { AnimeNewsNetworkWrapper.getMediaById(it) }

    override suspend fun getEpisodesByMediaId(id: Int) = MapCache.getOrComputeAsync(
        "AnimeNewsNetworkCachedWrapper.getEpisodesByMediaId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = id
    ) { AnimeNewsNetworkWrapper.getEpisodesByMediaId(it) }
}