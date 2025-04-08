package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.time.LocalDate
import java.util.*

data class CountryCodeLocalDateKeyCache(
    val countryCode: CountryCode,
    val member: UUID?,
    val localDate: LocalDate,
    val searchTypes: Array<LangType>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeLocalDateKeyCache) return false

        if (countryCode != other.countryCode) return false
        if (member != other.member) return false
        if (localDate != other.localDate) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + (member?.hashCode() ?: 0)
        result = 31 * result + localDate.hashCode()
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        return result
    }
}