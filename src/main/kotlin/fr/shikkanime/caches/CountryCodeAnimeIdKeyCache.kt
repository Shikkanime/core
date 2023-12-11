package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode

data class CountryCodeAnimeIdKeyCache(
    val countryCode: CountryCode,
    val animeId: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CountryCodeAnimeIdKeyCache

        if (countryCode != other.countryCode) return false
        if (animeId != other.animeId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + animeId.hashCode()
        return result
    }
}
