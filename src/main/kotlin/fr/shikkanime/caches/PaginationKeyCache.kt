package fr.shikkanime.caches

open class PaginationKeyCache(
    open val page: Int,
    open val limit: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaginationKeyCache) return false
        if (page != other.page) return false
        if (limit != other.limit) return false
        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + limit
        return result
    }
}
