package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.PageableCacheKey
import fr.shikkanime.caches.contracts.UuidCacheKey
import java.util.*

data class UuidPageCacheKey(
    override val uuid: UUID,
    override val page: Int,
    override val limit: Int,
) : PageableCacheKey, UuidCacheKey {
    override fun toString(): String {
        return "$uuid,$page,$limit"
    }
}
