package fr.shikkanime.caches

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

data class CountryCodeUUIDSortPaginationKeyCache(
    override val countryCode: CountryCode?,
    val uuid: UUID?,
    val sort: List<SortParameter>,
    override val page: Int,
    override val limit: Int,
    val status: Status? = null,
) : CountryCodePaginationKeyCache(countryCode, page, limit)
