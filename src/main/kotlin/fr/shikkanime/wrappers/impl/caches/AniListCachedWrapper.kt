package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.AniListWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration

object AniListCachedWrapper : AbstractAniListWrapper() {
    private val defaultCacheDuration = Duration.ofDays(1)

    override suspend fun search(
        query: String,
        page: Int,
        limit: Int
    ) = MapCache.getOrCompute(
        "AniListCachedWrapper.search",
        duration = defaultCacheDuration,
        key = Triple(query, page, limit)
    ) { (query, page, limit) -> runBlocking { AniListWrapper.search(query, page, limit) } }
}