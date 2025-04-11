package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.time.ZonedDateTime
import java.util.*

data class CountryCodeMemberUUIDWeekKeyCache(
    val countryCode: CountryCode,
    val member: UUID?,
    val startZonedDateTime: ZonedDateTime,
    val endZonedDateTime: ZonedDateTime,
    val searchTypes: Array<LangType>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountryCodeMemberUUIDWeekKeyCache) return false

        if (countryCode != other.countryCode) return false
        if (member != other.member) return false
        if (startZonedDateTime != other.startZonedDateTime) return false
        if (endZonedDateTime != other.endZonedDateTime) return false
        if (!searchTypes.contentEquals(other.searchTypes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = countryCode.hashCode()
        result = 31 * result + (member?.hashCode() ?: 0)
        result = 31 * result + startZonedDateTime.hashCode()
        result = 31 * result + endZonedDateTime.hashCode()
        result = 31 * result + (searchTypes?.contentHashCode() ?: 0)
        return result
    }
}