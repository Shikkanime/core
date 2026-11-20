package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.ApiCacheEntry

/**
 * Application service persisting the cached payloads of external API requests.
 *
 * Each method runs inside a single database transaction. Consume it through
 * [TransactionalProxy] or the Koin bindings so the [Transactional] methods are actually
 * intercepted.
 *
 * @see [ApiCacheEntry]
 */
interface ApiCacheService {
    /**
     * Finds the cached payload of one request.
     *
     * @param key identifier of the cached request.
     * @return the cached entry, or `null` when the request was never answered.
     * @see [ApiCacheEntry]
     */
    @Transactional
    fun findByKey(key: String): ApiCacheEntry?

    /**
     * Upserts the cached payload of one request, keyed by its identifier.
     *
     * @param entry cache entry to persist.
     * @return the entry as stored in database.
     * @see [ApiCacheEntry]
     */
    @Transactional
    fun save(entry: ApiCacheEntry): ApiCacheEntry
}
