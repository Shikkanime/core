package fr.shikkanime.database.entities

import fr.shikkanime.models.ApiCacheEntry
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

internal const val API_CACHE_TABLE_NAME = "api_cache"

internal object ApiCacheTable : ShikkTable(API_CACHE_TABLE_NAME) {
    val key = varchar("key", 512).uniqueIndex()
    val body = text("body")
}

internal class ApiCacheEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, ApiCacheTable),
    ModelConvertible<ApiCacheEntry> {
    companion object : UuidEntityClass<ApiCacheEntity>(ApiCacheTable)

    var key by ApiCacheTable.key
    var body by ApiCacheTable.body

    override fun toModel(): ApiCacheEntry =
        ApiCacheEntry(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            key = key,
            body = body
        )
}
