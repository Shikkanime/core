package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.ReleaseDayPlatformSimulcast

data class CountryCodeReleaseDayPlatformSimulcastKeyCache(
    val countryCode: CountryCode,
    val releaseDayPlatformSimulcast: ReleaseDayPlatformSimulcast
)