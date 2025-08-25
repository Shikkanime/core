package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType

data class CountryCodeNamePaginationKeyCache(
    override val countryCode: CountryCode?,
    val name: String,
    override val page: Int,
    override val limit: Int,
    val searchTypes: Array<LangType>?
) : CountryCodePaginationKeyCache(countryCode, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCodeNamePaginationKeyCache

        if (page != other.page) return false
        if (limit != other.limit) return false
        if (countryCode != other.countryCode) return false
        if (name != other.name) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + limit
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "$countryCode,'$name',$page,$limit,${searchTypes?.contentToString()}"
    }
}