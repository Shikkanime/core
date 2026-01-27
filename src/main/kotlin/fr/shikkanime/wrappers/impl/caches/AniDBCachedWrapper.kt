package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractAniDBWrapper
import fr.shikkanime.wrappers.impl.AniDBWrapper

object AniDBCachedWrapper : AbstractAniDBWrapper() {
    override suspend fun getEpisodesByMediaId(id: Int) = MapCache.getOrComputeAsync(
        "AniDBCachedWrapper.getEpisodesByMediaId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = id
    ) { AniDBWrapper.getEpisodesByMediaId(it) }
}