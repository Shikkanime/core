package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import java.time.ZonedDateTime

data class CountryCodePlatformWeekKeyCache(
    val countryCode: CountryCode,
    val platform: Platform,
    val startZonedDateTime: ZonedDateTime,
    val endZonedDateTime: ZonedDateTime
)