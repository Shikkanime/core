package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.NetflixConfiguration

data class CountryCodeNetflixSimulcastKeyCache(
    val countryCode: CountryCode,
    val netflixSimulcast: NetflixConfiguration.NetflixSimulcastDay
)