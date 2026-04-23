package fr.shikkanime.caches

import java.util.*

data class UUIDPaginationKeyCache(
    val uuid: UUID,
    override val page: Int,
    override val limit: Int,
) : PaginationKeyCache(page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UUIDPaginationKeyCache) return false
        if (!super.equals(other)) return false
        if (uuid != other.uuid) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    override fun toString(): String {
        return "$uuid,$page,$limit"
    }
}
