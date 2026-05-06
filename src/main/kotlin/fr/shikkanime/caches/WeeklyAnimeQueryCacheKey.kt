package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.CountryCacheKey
import fr.shikkanime.caches.contracts.SearchTypesCacheKey
import fr.shikkanime.caches.contracts.UuidCacheKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.time.LocalDate
import java.util.*

data class WeeklyAnimeQueryCacheKey(
    override val countryCode: CountryCode,
    override val uuid: UUID?,
    val weekStartDate: LocalDate,
    override val searchTypes: Array<LangType>? = null,
) : CountryCacheKey, SearchTypesCacheKey, UuidCacheKey {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeeklyAnimeQueryCacheKey) return false

        if (countryCode != other.countryCode) return false
        if (uuid != other.uuid) return false
        if (!weekStartDate.isEqual(other.weekStartDate)) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + weekStartDate.hashCode()
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "$countryCode,$uuid,$weekStartDate,${searchTypes.contentToString()}"
    }
}
