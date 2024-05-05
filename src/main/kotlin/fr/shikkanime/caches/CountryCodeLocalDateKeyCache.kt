package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import java.time.LocalDate
import java.util.*

data class CountryCodeLocalDateKeyCache(
    val member: UUID?,
    val countryCode: CountryCode,
    val localDate: LocalDate
)