package fr.shikkanime.caches

import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

data class CountryCodeUUIDSortPaginationKeyCache(
    val countryCode: CountryCode?,
    val uuid: UUID?,
    val sort: List<SortParameter>,
    val page: Int,
    val limit: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCodeUUIDSortPaginationKeyCache

        if (countryCode != other.countryCode) return false
        if (uuid != other.uuid) return false
        if (sort != other.sort) return false
        if (page != other.page) return false
        if (limit != other.limit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode?.hashCode() ?: 0
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + sort.hashCode()
        result = 31 * result + page
        result = 31 * result + limit
        return result
    }
}
