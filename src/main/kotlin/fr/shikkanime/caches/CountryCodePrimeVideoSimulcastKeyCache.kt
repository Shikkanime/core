package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.configuration.PrimeVideoConfiguration

data class CountryCodePrimeVideoSimulcastKeyCache(
    val countryCode: CountryCode,
    val primeVideoSimulcast: PrimeVideoConfiguration.PrimeVideoSimulcast
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCodePrimeVideoSimulcastKeyCache

        if (countryCode != other.countryCode) return false
        if (primeVideoSimulcast != other.primeVideoSimulcast) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + primeVideoSimulcast.hashCode()
        return result
    }
}
