package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType

data class CountryCodeNamePaginationKeyCache(
    override val countryCode: CountryCode?,
    val name: String,
    override val page: Int,
    override val limit: Int,
    val searchTypes: List<LangType>?
) : CountryCodePaginationKeyCache(countryCode, page, limit)