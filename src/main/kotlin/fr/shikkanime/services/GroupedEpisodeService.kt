package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.repositories.GroupedEpisodeRepository

class GroupedEpisodeService : AbstractService<EpisodeMapping, GroupedEpisodeRepository>() {
    @Inject private lateinit var groupedEpisodeRepository: GroupedEpisodeRepository

    override fun getRepository() = groupedEpisodeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = groupedEpisodeRepository.findAllBy(countryCode, searchTypes, sort, page, limit)
}
