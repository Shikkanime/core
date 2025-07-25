package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

object AnimationDigitalNetworkCachedWrapper : AbstractAnimationDigitalNetworkWrapper() {
    private val videoCache = MapCache<Int, Video>("AnimationDigitalNetworkCachedWrapper.videoCache") {
        runBlocking { AnimationDigitalNetworkWrapper.getVideo(it) }
    }

    override suspend fun getLatestVideos(date: LocalDate) = MapCache.getOrCompute(
        "AnimationDigitalNetworkCachedWrapper.getLatestVideos",
        key = date
    ) {
        runBlocking { AnimationDigitalNetworkWrapper.getLatestVideos(it) }
            .apply { forEach { video -> videoCache.setIfNotExists(video.id, video) } }
    }

    override suspend fun getShow(id: Int) = MapCache.getOrCompute(
        "AnimationDigitalNetworkCachedWrapper.getShow",
        key = id
    ) { runBlocking { AnimationDigitalNetworkWrapper.getShow(it) } }

    override suspend fun getShowVideos(id: Int) = MapCache.getOrCompute(
        "AnimationDigitalNetworkCachedWrapper.getShowVideos",
        key = id
    ) { runBlocking { AnimationDigitalNetworkWrapper.getShowVideos(it) } }

    override suspend fun getVideo(id: Int) = videoCache[id] ?: throw Exception("Video not found")
}