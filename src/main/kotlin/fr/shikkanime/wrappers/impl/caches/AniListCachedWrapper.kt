package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.AniListWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration

object AniListCachedWrapper : AbstractAniListWrapper() {
    private data class AniListCacheKey(val query: String, val page: Int, val limit: Int, val status: List<Status>)

    private val defaultCacheDuration = Duration.ofDays(1)

    override suspend fun search(
        query: String,
        page: Int,
        limit: Int,
        status: List<Status>
    ) = MapCache.getOrCompute(
        "AniListCachedWrapper.search",
        typeToken = object : TypeToken<MapCacheValue<Array<Media>>>() {},
        duration = defaultCacheDuration,
        key = AniListCacheKey(query, page, limit, status)
    ) { (query, page, limit, status) -> runBlocking { AniListWrapper.search(query, page, limit, status) } }
}