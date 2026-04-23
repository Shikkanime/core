package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import java.util.*

data class CountryCodeUUIDSeasonSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    val uuid: UUID?,
    val season: Int?,
    override val searchTypes: Array<LangType>?,
    override val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
) : CountryCodeSortPaginationKeyCache(countryCode, searchTypes, sort, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeUUIDSeasonSortPaginationKeyCache) return false
        if (!super.equals(other)) return false
        if (uuid != other.uuid) return false
        if (season != other.season) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + (season ?: 0)
        return result
    }

    override fun toString(): String {
        return "$countryCode,$uuid,$season,${searchTypes?.contentToString()},$sort,$page,$limit"
    }
}
