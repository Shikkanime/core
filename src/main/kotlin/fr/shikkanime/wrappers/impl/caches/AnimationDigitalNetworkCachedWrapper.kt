package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate

object AnimationDigitalNetworkCachedWrapper : AbstractAnimationDigitalNetworkWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    private val videoCache = MapCache<Int, Video>(defaultCacheDuration) {
        runBlocking { AnimationDigitalNetworkWrapper.getVideo(it) }
    }

    private val latestVideosCache = MapCache<LocalDate, Array<Video>>(defaultCacheDuration) {
        runBlocking { AnimationDigitalNetworkWrapper.getLatestVideos(it) }
            .apply { forEach { video -> videoCache.setIfNotExists(video.id, video) } }
    }

    private val showCache = MapCache<Int, Show>(defaultCacheDuration) {
        runBlocking { AnimationDigitalNetworkWrapper.getShow(it) }
    }

    private val showVideosCache = MapCache<Int, Array<Video>>(defaultCacheDuration) {
        runBlocking { AnimationDigitalNetworkWrapper.getShowVideos(it) }
            .apply { forEach { video -> videoCache.setIfNotExists(video.id, video) } }
    }

    override suspend fun getLatestVideos(date: LocalDate) = latestVideosCache[date] ?: emptyArray()

    override suspend fun getShow(id: Int) = showCache[id] ?: throw Exception("Show not found")

    override suspend fun getShowVideos(id: Int) = showVideosCache[id] ?: emptyArray()

    override suspend fun getVideo(id: Int) = videoCache[id] ?: throw Exception("Video not found")
}