package fr.shikkanime.caches

import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Status
import java.util.*

data class CountryCodeUUIDSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    val uuid: UUID?,
    val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
    val status: Status? = null,
) : CountryCodePaginationKeyCache(countryCode, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as CountryCodeUUIDSortPaginationKeyCache

        if (countryCode != other.countryCode) return false
        if (uuid != other.uuid) return false
        if (sort != other.sort) return false
        if (page != other.page) return false
        if (limit != other.limit) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + sort.hashCode()
        result = 31 * result + page
        result = 31 * result + limit
        result = 31 * result + (status?.hashCode() ?: 0)
        return result
    }
}
