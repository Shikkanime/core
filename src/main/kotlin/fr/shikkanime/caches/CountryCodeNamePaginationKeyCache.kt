package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

data class CountryCodeNamePaginationKeyCache(
    override val countryCode: CountryCode?,
    val name: String,
    override val page: Int,
    override val limit: Int,
) : CountryCodePaginationKeyCache(countryCode, page, limit) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as CountryCodeNamePaginationKeyCache

        if (countryCode != other.countryCode) return false
        if (name != other.name) return false
        if (page != other.page) return false
        if (limit != other.limit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + page
        result = 31 * result + limit
        return result
    }
}
