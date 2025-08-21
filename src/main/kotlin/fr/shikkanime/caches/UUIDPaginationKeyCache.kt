package fr.shikkanime.caches

import java.util.*

data class UUIDPaginationKeyCache(
    val uuid: UUID,
    val page: Int,
    val limit: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UUIDPaginationKeyCache) return false

        if (page != other.page) return false
        if (limit != other.limit) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + limit
        result = 31 * result + uuid.hashCode()
        return result
    }

    override fun toString(): String {
        return "$uuid,$page,$limit"
    }
}
