package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import fr.shikkanime.wrappers.impl.PrimeVideoWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration

object PrimeVideoCachedWrapper : AbstractPrimeVideoWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    override suspend fun getEpisodesByShowId(
        locale: String,
        id: String
    ) = MapCache.getOrCompute(
        "PrimeVideoCachedWrapper.getEpisodesByShowId",
        duration = defaultCacheDuration,
        key = locale to id
    ) { runBlocking { PrimeVideoWrapper.getEpisodesByShowId(it.first, it.second) } }
}