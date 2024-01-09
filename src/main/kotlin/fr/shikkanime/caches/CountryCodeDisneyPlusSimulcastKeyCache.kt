package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration

data class CountryCodeDisneyPlusSimulcastKeyCache(
    val countryCode: CountryCode,
    val disneyPlusSimulcast: DisneyPlusConfiguration.DisneyPlusSimulcast
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCodeDisneyPlusSimulcastKeyCache

        if (countryCode != other.countryCode) return false
        if (disneyPlusSimulcast != other.disneyPlusSimulcast) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + disneyPlusSimulcast.hashCode()
        return result
    }
}
