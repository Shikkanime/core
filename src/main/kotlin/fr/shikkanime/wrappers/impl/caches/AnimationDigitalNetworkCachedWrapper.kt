package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import java.time.LocalDate

object AnimationDigitalNetworkCachedWrapper : AbstractAnimationDigitalNetworkWrapper() {
    private val videoCache = MapCache<Pair<CountryCode, Int>, Video>(
        "AnimationDigitalNetworkCachedWrapper.videoCache",
        typeToken = object : TypeToken<MapCacheValue<Video>>() {}
    ) { (countryCode, id) -> AnimationDigitalNetworkWrapper.getVideo(countryCode, id) }

    override suspend fun getLatestVideos(countryCode: CountryCode, date: LocalDate) = MapCache.getOrComputeAsync(
        "AnimationDigitalNetworkCachedWrapper.getLatestVideos",
        typeToken = object : TypeToken<MapCacheValue<Array<Video>>>() {},
        key = countryCode to date
    ) { (countryCode, date) ->
        AnimationDigitalNetworkWrapper.getLatestVideos(countryCode, date)
            .apply { forEach { video -> videoCache.putIfNotExists(countryCode to video.id, video) } }
    }

    override suspend fun getShow(country: String, id: Int) = MapCache.getOrComputeAsync(
        "AnimationDigitalNetworkCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = country to id
    ) { (country, id) -> AnimationDigitalNetworkWrapper.getShow(country, id) }

    override suspend fun getShowVideos(countryCode: CountryCode, id: Int) = MapCache.getOrComputeAsync(
        "AnimationDigitalNetworkCachedWrapper.getShowVideos",
        typeToken = object : TypeToken<MapCacheValue<Array<Video>>>() {},
        key = countryCode to id
    ) { (countryCode, id) -> AnimationDigitalNetworkWrapper.getShowVideos(countryCode, id) }

    override suspend fun getVideo(countryCode: CountryCode, id: Int) = videoCache[countryCode to id] ?: throw Exception("Video not found")
}