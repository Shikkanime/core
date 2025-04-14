package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.views.GroupedEpisodeView
import fr.shikkanime.repositories.GroupedEpisodeRepository

class GroupedEpisodeService : AbstractService<GroupedEpisodeView, GroupedEpisodeRepository>() {
    @Inject private lateinit var groupedEpisodeRepository: GroupedEpisodeRepository

    override fun getRepository() = groupedEpisodeRepository

    fun findAllBy(
        countryCode: CountryCode,
        page: Int,
        limit: Int,
    ) = groupedEpisodeRepository.findAllBy(countryCode, page, limit)
}