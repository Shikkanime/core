package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

open class CountryCodePaginationKeyCache(
    open val countryCode: CountryCode?,
    open val page: Int,
    open val limit: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodePaginationKeyCache) return false

        if (page != other.page) return false
        if (limit != other.limit) return false
        if (countryCode != other.countryCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + limit
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        return result
    }
}
