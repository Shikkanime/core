package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.miscellaneous.SortParameter

open class CountryCodeSortPaginationKeyCache(
    open val countryCode: CountryCode?,
    val sort: List<SortParameter>,
    open val page: Int,
    open val limit: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeSortPaginationKeyCache) return false

        if (page != other.page) return false
        if (limit != other.limit) return false
        if (countryCode != other.countryCode) return false
        if (sort != other.sort) return false

        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + limit
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + sort.hashCode()
        return result
    }
}
