package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration

data class CountryCodeDisneyPlusSimulcastKeyCache(
    val countryCode: CountryCode,
    val disneyPlusSimulcast: DisneyPlusConfiguration.DisneyPlusSimulcast
)