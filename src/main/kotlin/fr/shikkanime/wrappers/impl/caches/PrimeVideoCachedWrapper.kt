package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import fr.shikkanime.wrappers.impl.PrimeVideoWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration

object PrimeVideoCachedWrapper : AbstractPrimeVideoWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    private val showVideosCache = MapCache<Pair<CountryCode, String>, List<Episode>?>(
        "PrimeVideoCachedWrapper.showVideosCache",
        duration = defaultCacheDuration
    ) {
        runBlocking { PrimeVideoWrapper.getShowVideos(it.first, it.second) }
    }

    override fun getShowVideos(countryCode: CountryCode, showId: String) = showVideosCache[countryCode to showId]
}