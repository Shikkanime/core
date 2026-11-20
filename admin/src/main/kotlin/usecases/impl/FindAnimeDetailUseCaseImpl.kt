package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.admin.dtos.anime.AdminAnimeDetailDto
import fr.shikkanime.admin.dtos.anime.AnimeWithMetadataLinks
import fr.shikkanime.admin.dtos.anime.toAdminDetailDto
import fr.shikkanime.admin.usecases.FindAnimeDetailUseCase
import fr.shikkanime.database.services.AnimeMetadataLinkService
import fr.shikkanime.database.services.AnimeService
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class FindAnimeDetailUseCaseImpl(
    private val animeService: AnimeService,
    private val animeMetadataLinkService: AnimeMetadataLinkService
) : FindAnimeDetailUseCase {
    override fun findById(id: Uuid): AdminAnimeDetailDto? {
        // Read the stored anime first: without it there is nothing to join the links to
        val anime = animeService.findById(id) ?: return null

        // Resolve the metadata platform links of this anime for the detail view
        val metadataLinks = animeMetadataLinkService
            .findAllByAnimeIds(listOf(id))[id].orEmpty()

        return AnimeWithMetadataLinks(anime, metadataLinks).toAdminDetailDto()
    }
}
