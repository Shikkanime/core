package fr.shikkanime.jobs

import fr.shikkanime.cache.Cache
import fr.shikkanime.database.cache.CacheBuckets
import org.koin.core.annotation.Single

/**
 * Drops the catalog entries the API instances serve after a harvest wrote the database.
 *
 * The invalidation runs once per harvest, after every save transaction committed: incrementing the
 * bucket version makes all the entries written under the previous version unreachable, and the
 * entries themselves expire with their TTL.
 *
 * @see [Cache.invalidate]
 * @see [CacheBuckets]
 */
@Single
internal class CatalogCacheInvalidator(
    private val cache: Cache
) {
    /**
     * Invalidates the anime and episode buckets.
     *
     * @return nothing; the version counters are incremented in Valkey.
     * @see [Cache.invalidate]
     */
    suspend fun invalidate() {
        cache.invalidate(CacheBuckets.ANIME)
        cache.invalidate(CacheBuckets.EPISODE)
    }
}
