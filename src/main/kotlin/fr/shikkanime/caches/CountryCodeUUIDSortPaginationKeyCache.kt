package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import java.util.*

data class CountryCodeUUIDSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    val simulcastUuid: UUID?,
    val name: String?,
    override val searchTypes: Array<LangType>?,
    override val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
) : CountryCodeSortPaginationKeyCache(countryCode, searchTypes, sort, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeUUIDSortPaginationKeyCache) return false
        if (!super.equals(other)) return false
        if (simulcastUuid != other.simulcastUuid) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (simulcastUuid?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "$countryCode,$simulcastUuid,$name,${searchTypes.contentToString()},$sort,$page,$limit"
    }
}
