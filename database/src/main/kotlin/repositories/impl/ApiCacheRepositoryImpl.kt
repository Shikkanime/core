package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.ApiCacheEntity
import fr.shikkanime.database.entities.ApiCacheTable
import fr.shikkanime.database.repositories.ApiCacheRepository
import fr.shikkanime.models.ApiCacheEntry
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [ApiCacheRepository::class])
internal class ApiCacheRepositoryImpl : ApiCacheRepository() {

    /**
     * Finds the cached payload of one request.
     *
     * @param key identifier of the cached request.
     * @return the matching entity, or `null` when the request was never answered.
     * @see [ApiCacheEntry]
     */
    private fun findEntityByKey(key: String): ApiCacheEntity? =
        ApiCacheEntity.find { ApiCacheTable.key eq key }
            .limit(1)
            .firstOrNull()

    override fun findByKey(key: String): ApiCacheEntry? =
        findEntityByKey(key)?.toModel()

    override fun save(entry: ApiCacheEntry): ApiCacheEntry =
        findEntityByKey(entry.key)
            .ifExists {
                if (body != entry.body) {
                    body = entry.body
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(entry.id) {
                key = entry.key
                body = entry.body
            }
            .applyFlush()
            .toModel()
}
