package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.ApiCacheEntity
import fr.shikkanime.models.ApiCacheEntry
import kotlin.uuid.Uuid

/**
 * Persistence port of the [ApiCacheEntry] payloads, keyed by request identifier.
 *
 * @see [ApiCacheEntry]
 */
internal abstract class ApiCacheRepository :
    ModelRepository<Uuid, ApiCacheEntity, ApiCacheEntry>(ApiCacheEntity) {
    /**
     * Finds the cached payload of one request.
     *
     * @param key identifier of the cached request.
     * @return the cached entry, or `null` when the request was never answered.
     * @see [ApiCacheEntry]
     */
    abstract fun findByKey(key: String): ApiCacheEntry?

    /**
     * Upserts the cached payload of one request, keyed by its identifier.
     *
     * A known key has its body and timestamp refreshed; a new key is inserted.
     *
     * @param entry cache entry to persist.
     * @return the entry as stored in database.
     * @see [ApiCacheEntry]
     */
    abstract fun save(entry: ApiCacheEntry): ApiCacheEntry
}
