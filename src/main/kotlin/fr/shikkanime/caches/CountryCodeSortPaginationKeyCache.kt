package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter

open class CountryCodeSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    open val searchTypes: Array<LangType>? = null,
    open val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
) : CountryCodePaginationKeyCache(countryCode, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeSortPaginationKeyCache) return false
        if (!super.equals(other)) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false
        if (sort != other.sort) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        result = 31 * result + sort.hashCode()
        return result
    }

    override fun toString() = "$countryCode,${searchTypes.contentToString()},$sort,$page,$limit"
}
