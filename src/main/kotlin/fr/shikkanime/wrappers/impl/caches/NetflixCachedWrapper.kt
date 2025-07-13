package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.ZonedDateTime

object NetflixCachedWrapper : AbstractNetflixWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    override suspend fun getShow(
        locale: String,
        id: Int
    ) = MapCache.getOrCompute(
        "NetflixCachedWrapper.getShow",
        duration = defaultCacheDuration,
        key = locale to id
    ) { runBlocking { NetflixWrapper.getShow(it.first, it.second) } }

    override suspend fun getEpisodesByShowId(
        zonedDateTime: ZonedDateTime,
        locale: String,
        id: Int,
        fetchLocaleAfterReleaseDateTime: Boolean
    ) = MapCache.getOrCompute(
        "NetflixCachedWrapper.getEpisodesByShowId",
        duration = defaultCacheDuration,
        key = locale to id
    ) { runBlocking { NetflixWrapper.getEpisodesByShowId(zonedDateTime, it.first, it.second, fetchLocaleAfterReleaseDateTime) } }

    override suspend fun getEpisodeAudioLocalesAndSubtitles(id: Int) = MapCache.getOrComputeNullable(
        "NetflixCachedWrapper.getEpisodeAudioLocalesAndSubtitles",
        duration = defaultCacheDuration,
        key = id
    ) { runBlocking { NetflixWrapper.getEpisodeAudioLocalesAndSubtitles(it) } }
}