package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

open class CountryCodePaginationKeyCache(
    open val countryCode: CountryCode?,
    override val page: Int,
    override val limit: Int,
) : PaginationKeyCache(page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodePaginationKeyCache) return false
        if (!super.equals(other)) return false
        if (countryCode != other.countryCode) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        return result
    }
}
