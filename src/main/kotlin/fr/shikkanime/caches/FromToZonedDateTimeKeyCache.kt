package fr.shikkanime.caches

import java.time.ZonedDateTime

data class FromToZonedDateTimeKeyCache(
    val from: ZonedDateTime,
    val to: ZonedDateTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FromToZonedDateTimeKeyCache) return false

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }
}
