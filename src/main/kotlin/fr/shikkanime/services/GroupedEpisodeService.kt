package fr.shikkanime.services

import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.repositories.GroupedEpisodeRepository

class GroupedEpisodeService : AbstractService<EpisodeMapping, GroupedEpisodeRepository>() {
    fun findAllBy(
        countryCode: CountryCode?,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = repository.findAllBy(countryCode, searchTypes, sort, page, limit)
}
