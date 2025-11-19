package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper

object NetflixCachedWrapper : AbstractNetflixWrapper() {
    override suspend fun getShow(
        locale: String,
        id: Int
    ) = MapCache.getOrComputeAsync(
        "NetflixCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = locale to id
    ) { NetflixWrapper.getShow(it.first, it.second) }

    override suspend fun getEpisodesByShowId(
        locale: String,
        id: Int
    ) = MapCache.getOrComputeAsync(
        "NetflixCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = locale to id
    ) { NetflixWrapper.getEpisodesByShowId(it.first, it.second) }
}