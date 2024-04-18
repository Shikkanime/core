package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

data class CountryCodeAnimeIdKeyCache(
    val countryCode: CountryCode,
    val animeId: String,
)
