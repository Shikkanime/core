package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import java.time.LocalDate

data class CountryCodeLocalDateKeyCache(
    val countryCode: CountryCode,
    val localDate: LocalDate
)