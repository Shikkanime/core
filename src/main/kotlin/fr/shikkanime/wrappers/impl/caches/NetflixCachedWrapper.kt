package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration

object NetflixCachedWrapper : AbstractNetflixWrapper() {
    data class Key(
        val countryCode: CountryCode,
        val showId: String,
        val seasonName: String,
        val season: Int
    )

    private val defaultCacheDuration = Duration.ofDays(1)

    override fun getShowVideos(
        countryCode: CountryCode,
        showId: String,
        seasonName: String,
        season: Int
    ) = MapCache.getOrCompute(
        "NetflixCachedWrapper.getShowVideos",
        duration = defaultCacheDuration,
        key = Key(countryCode, showId, seasonName, season)
    ) { runBlocking { NetflixWrapper.getShowVideos(it.countryCode, it.showId, it.seasonName, it.season) } }
}