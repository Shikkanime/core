package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

open class CountryCodePaginationKeyCache(
    open val countryCode: CountryCode?,
    open val page: Int,
    open val limit: Int,
)
