package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.CountryCacheKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import java.time.ZonedDateTime

data class PlatformReleaseWindowCacheKey(
    override val countryCode: CountryCode,
    val platform: Platform,
    val windowStart: ZonedDateTime,
    val windowEnd: ZonedDateTime
) : CountryCacheKey
