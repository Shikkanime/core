package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

data class CountryCodeNamePaginationKeyCache(
    override val countryCode: CountryCode?,
    val name: String,
    override val page: Int,
    override val limit: Int,
) : CountryCodePaginationKeyCache(countryCode, page, limit)