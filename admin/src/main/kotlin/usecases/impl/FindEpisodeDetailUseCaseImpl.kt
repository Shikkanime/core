package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.admin.dtos.episode.AdminEpisodeDetailDto
import fr.shikkanime.admin.dtos.episode.EpisodeWithMetadataLinks
import fr.shikkanime.admin.dtos.episode.toAdminDetailDto
import fr.shikkanime.admin.usecases.FindEpisodeDetailUseCase
import fr.shikkanime.database.services.EpisodeMetadataLinkService
import fr.shikkanime.database.services.EpisodeService
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class FindEpisodeDetailUseCaseImpl(
    private val episodeService: EpisodeService,
    private val episodeMetadataLinkService: EpisodeMetadataLinkService
) : FindEpisodeDetailUseCase {
    override fun findById(id: Uuid): AdminEpisodeDetailDto? {
        // Read the stored episode first: without it there is nothing to join the links to
        val episode = episodeService.findById(id) ?: return null

        // Resolve the metadata platform links of this episode for the detail view
        val metadataLinks = episodeMetadataLinkService
            .findAllByEpisodeIds(listOf(id))[id].orEmpty()

        return EpisodeWithMetadataLinks(episode, metadataLinks).toAdminDetailDto()
    }
}
