package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

object AnimationDigitalNetworkCachedWrapper : AbstractAnimationDigitalNetworkWrapper() {
    private val videoCache = MapCache<Pair<CountryCode, Int>, Video>(
        "AnimationDigitalNetworkCachedWrapper.videoCache",
        typeToken = object : TypeToken<MapCacheValue<Video>>() {}
    ) { (countryCode, id) -> runBlocking { AnimationDigitalNetworkWrapper.getVideo(countryCode, id) } }

    override suspend fun getLatestVideos(countryCode: CountryCode, date: LocalDate) = MapCache.getOrCompute(
        "AnimationDigitalNetworkCachedWrapper.getLatestVideos",
        typeToken = object : TypeToken<MapCacheValue<Array<Video>>>() {},
        key = countryCode to date
    ) { (countryCode, date) ->
        runBlocking { AnimationDigitalNetworkWrapper.getLatestVideos(countryCode, date) }
            .apply { forEach { video -> videoCache.putIfNotExists(countryCode to video.id, video) } }
    }

    override suspend fun getShow(countryCode: CountryCode, id: Int) = MapCache.getOrCompute(
        "AnimationDigitalNetworkCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = countryCode to id
    ) { (countryCode, id) -> runBlocking { AnimationDigitalNetworkWrapper.getShow(countryCode, id) } }

    override suspend fun getShowVideos(countryCode: CountryCode, id: Int) = MapCache.getOrCompute(
        "AnimationDigitalNetworkCachedWrapper.getShowVideos",
        typeToken = object : TypeToken<MapCacheValue<Array<Video>>>() {},
        key = countryCode to id
    ) { (countryCode, id) -> runBlocking { AnimationDigitalNetworkWrapper.getShowVideos(countryCode, id) } }

    override suspend fun getVideo(countryCode: CountryCode, id: Int) = videoCache[countryCode to id] ?: throw Exception("Video not found")
}