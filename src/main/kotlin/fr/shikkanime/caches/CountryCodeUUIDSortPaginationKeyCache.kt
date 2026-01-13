package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import java.util.*

data class CountryCodeUUIDSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    val simulcastUuid: UUID?,
    val name: String?,
    val searchTypes: Array<LangType>?,
    val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
) : CountryCodePaginationKeyCache(countryCode, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeUUIDSortPaginationKeyCache) return false
        if (!super.equals(other)) return false

        if (page != other.page) return false
        if (limit != other.limit) return false
        if (countryCode != other.countryCode) return false
        if (simulcastUuid != other.simulcastUuid) return false
        if (name != other.name) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false
        if (sort != other.sort) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + page
        result = 31 * result + limit
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + (simulcastUuid?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        result = 31 * result + sort.hashCode()
        return result
    }

    override fun toString(): String {
        return "$countryCode,$simulcastUuid,$name,${searchTypes.contentToString()},$sort,$page,$limit"
    }
}
