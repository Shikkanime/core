package fr.shikkanime.caches.contracts

import fr.shikkanime.entities.enums.CountryCode

interface CountryCacheKey {
    val countryCode: CountryCode?
}
