package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.miscellaneous.SortParameter
import java.util.*

data class CountryCodeUUIDSeasonSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    val uuid: UUID?,
    val season: Int?,
    val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
) : CountryCodePaginationKeyCache(countryCode, page, limit)
