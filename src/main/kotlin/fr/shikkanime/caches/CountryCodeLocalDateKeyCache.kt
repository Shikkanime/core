package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import java.time.LocalDate
import java.util.*

data class CountryCodeLocalDateKeyCache(
    val countryCode: CountryCode,
    val member: UUID?,
    val localDate: LocalDate
)