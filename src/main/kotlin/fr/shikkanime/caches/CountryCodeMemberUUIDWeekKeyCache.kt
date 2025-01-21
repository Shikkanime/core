package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import java.time.ZonedDateTime
import java.util.*

data class CountryCodeMemberUUIDWeekKeyCache(
    val countryCode: CountryCode,
    val member: UUID?,
    val startZonedDateTime: ZonedDateTime,
    val endZonedDateTime: ZonedDateTime
)