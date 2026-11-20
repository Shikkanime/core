package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.ApiCacheRepository
import fr.shikkanime.database.services.ApiCacheService
import fr.shikkanime.models.ApiCacheEntry
import org.koin.core.annotation.Single

@Suppress("unused")
@Single(binds = [ApiCacheService::class])
internal class ApiCacheServiceImpl(
    private val cacheRepository: ApiCacheRepository
) : ApiCacheService {

    override fun findByKey(key: String): ApiCacheEntry? =
        // Serve the cached payload of a remote call when its key is known
        cacheRepository.findByKey(key)

    override fun save(entry: ApiCacheEntry): ApiCacheEntry =
        // Record the fresh payload so the next call can be served without the remote call
        cacheRepository.save(entry)
}
