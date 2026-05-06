package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import java.time.LocalDate

object AnimationDigitalNetworkCachedWrapper : AbstractAnimationDigitalNetworkWrapper() {
    private val episodeCache = MapCache<Pair<String, Int>, Episode>(
        "AnimationDigitalNetworkCachedWrapper.episodeCache",
        typeToken = object : TypeToken<MapCacheValue<Episode>>() {}
    ) { (locale, id) -> AnimationDigitalNetworkWrapper.getEpisode(locale, id) }

    override suspend fun getLatestEpisodes(locale: String, date: LocalDate) = MapCache.getOrComputeAsync(
        "AnimationDigitalNetworkCachedWrapper.getLatestEpisodes",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = locale to date
    ) { (locale, date) ->
        AnimationDigitalNetworkWrapper.getLatestEpisodes(locale, date)
            .apply { forEach { episode -> episodeCache[locale to episode.id] = episode } }
    }

    override suspend fun getShow(locale: String, id: Int) = MapCache.getOrComputeAsync(
        "AnimationDigitalNetworkCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = locale to id
    ) { (locale, id) -> AnimationDigitalNetworkWrapper.getShow(locale, id) }

    override suspend fun getEpisodesByShowId(locale: String, showId: Int) = MapCache.getOrComputeAsync(
        "AnimationDigitalNetworkCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = locale to showId
    ) { (locale, showId) -> AnimationDigitalNetworkWrapper.getEpisodesByShowId(locale, showId) }

    override suspend fun getEpisode(locale: String, id: Int) = episodeCache[locale to id] ?: throw Exception("Episode not found")
}