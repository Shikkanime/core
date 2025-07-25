package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import kotlinx.coroutines.runBlocking

object NetflixCachedWrapper : AbstractNetflixWrapper() {
    override suspend fun getShow(
        locale: String,
        id: Int
    ) = MapCache.getOrCompute(
        "NetflixCachedWrapper.getShow",
        key = locale to id
    ) { runBlocking { NetflixWrapper.getShow(it.first, it.second) } }

    override suspend fun getEpisodesByShowId(
        locale: String,
        id: Int
    ) = MapCache.getOrCompute(
        "NetflixCachedWrapper.getEpisodesByShowId",
        key = locale to id
    ) { runBlocking { NetflixWrapper.getEpisodesByShowId(it.first, it.second) } }
}