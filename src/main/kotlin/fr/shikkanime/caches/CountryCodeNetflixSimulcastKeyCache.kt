package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.NetflixConfiguration

data class CountryCodeNetflixSimulcastKeyCache(val countryCode: CountryCode, val netflixSimulcast: NetflixConfiguration.NetflixSimulcast) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCodeNetflixSimulcastKeyCache

        if (countryCode != other.countryCode) return false
        if (netflixSimulcast != other.netflixSimulcast) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + netflixSimulcast.hashCode()
        return result
    }
}
