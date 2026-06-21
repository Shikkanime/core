package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import fr.shikkanime.wrappers.impl.NetflixWrapper

object NetflixCachedWrapper : AbstractNetflixWrapper() {
    private val episodeAudioTrackListCache = MapCache<Pair<String, Int>, Array<String>>(
        "NetflixCachedWrapper.episodeAudioTrackListCache",
        typeToken = object : TypeToken<MapCacheValue<Array<String>>>() {}
    ) { TODO("Not yet implemented") }

    override suspend fun getLatestShows() = MapCache.getOrComputeAsync(
        "NetflixCachedWrapper.getLatestShows",
        typeToken = object : TypeToken<MapCacheValue<Array<LatestShow>>>() {},
        key = StringUtils.EMPTY_STRING
    ) { NetflixWrapper.getLatestShows() }

    override suspend fun getShow(
        locale: String,
        id: Int
    ) = MapCache.getOrComputeAsync(
        "NetflixCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = locale to id
    ) { NetflixWrapper.getShow(it.first, it.second) }

    override suspend fun getEpisodesByShowId(
        locale: String,
        showId: Int,
        checkAudioLocales: Boolean
    ) = MapCache.getOrComputeAsync(
        "NetflixCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = Triple(locale, showId, checkAudioLocales)
    ) { NetflixWrapper.getEpisodesByShowId(it.first, it.second, it.third) }

    override suspend fun getEpisodeAudioTrackList(locale: String, vararg ids: Int): Map<Int, Set<String>> {
        val predicate: (Pair<String, Int>) -> Boolean = { episodeAudioTrackListCache.containsKey(it) }
        val alreadyCached = ids.filter { predicate(locale to it) }
        val notCached = ids.filterNot { predicate(locale to it) }
        val audioTrackMap = alreadyCached.associateWith { episodeAudioTrackListCache[locale to it]!!.toSet() }.toMutableMap()

         if (notCached.isNotEmpty()) {
            val newAudioTrackMap = NetflixWrapper.getEpisodeAudioTrackList(locale, *notCached.toIntArray())
             newAudioTrackMap.forEach { episodeAudioTrackListCache[locale to it.key] = it.value.toTypedArray() }
            audioTrackMap.putAll(newAudioTrackMap)
        }

        return audioTrackMap
    }
}