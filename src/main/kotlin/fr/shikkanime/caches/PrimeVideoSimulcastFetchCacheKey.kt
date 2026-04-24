package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.CountryCacheKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration

data class PrimeVideoSimulcastFetchCacheKey(
    override val countryCode: CountryCode,
    val simulcast: PrimeVideoConfiguration.PrimeVideoSimulcast
) : CountryCacheKey
