package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.CountryCacheKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.ReleaseDayPlatformSimulcast

data class PlatformSimulcastFetchCacheKey(
    override val countryCode: CountryCode,
    val simulcast: ReleaseDayPlatformSimulcast
) : CountryCacheKey
