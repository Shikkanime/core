package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.CountryPageableCacheKey
import fr.shikkanime.caches.contracts.SearchTypesCacheKey
import fr.shikkanime.caches.contracts.SortParametersCacheKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter

data class GroupedEpisodeQueryCacheKey(
    override val countryCode: CountryCode?,
    override val searchTypes: Array<LangType>? = null,
    override val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
) : CountryPageableCacheKey, SearchTypesCacheKey, SortParametersCacheKey {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupedEpisodeQueryCacheKey) return false
        if (countryCode != other.countryCode) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false
        if (sort != other.sort) return false
        if (page != other.page) return false
        if (limit != other.limit) return false
        return true
    }

    override fun hashCode(): Int {
        var result = countryCode?.hashCode() ?: 0
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        result = 31 * result + sort.hashCode()
        result = 31 * result + page
        result = 31 * result + limit
        return result
    }

    override fun toString() = "$countryCode,${searchTypes.contentToString()},$sort,$page,$limit"
}
