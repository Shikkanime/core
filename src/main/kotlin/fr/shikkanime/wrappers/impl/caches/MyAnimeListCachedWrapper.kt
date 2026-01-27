package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractMyAnimeListWrapper
import fr.shikkanime.wrappers.impl.MyAnimeListWrapper

object MyAnimeListCachedWrapper : AbstractMyAnimeListWrapper() {
    override suspend fun getMediaById(id: Int) = MapCache.getOrComputeAsync(
        "MyAnimeListCachedWrapper.getMediaById",
        typeToken = object : TypeToken<MapCacheValue<Media>>() {},
        key = id
    ) { MyAnimeListWrapper.getMediaById(it) }

    override suspend fun getEpisodesByMediaId(id: Int) = MapCache.getOrComputeAsync(
        "MyAnimeListCachedWrapper.getEpisodesByMediaId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = id
    ) { MyAnimeListWrapper.getEpisodesByMediaId(it) }
}