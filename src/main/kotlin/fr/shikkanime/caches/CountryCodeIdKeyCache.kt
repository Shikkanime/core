package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

data class CountryCodeIdKeyCache(
    val countryCode: CountryCode,
    val id: String,
)
