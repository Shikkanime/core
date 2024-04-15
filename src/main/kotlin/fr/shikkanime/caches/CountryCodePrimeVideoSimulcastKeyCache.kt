package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration

data class CountryCodePrimeVideoSimulcastKeyCache(
    val countryCode: CountryCode,
    val primeVideoSimulcast: PrimeVideoConfiguration.PrimeVideoSimulcast
)
