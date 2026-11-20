package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.AnimeMetadataLinkRepository
import fr.shikkanime.database.services.AnimeMetadataLinkService
import fr.shikkanime.models.AnimeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimeMetadataLinkService::class])
internal class AnimeMetadataLinkServiceImpl(
    private val linkRepository: AnimeMetadataLinkRepository
) : AnimeMetadataLinkService {

    override fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimeMetadataLink>> =
        // Resolve every metadata link of the page in one grouped read
        linkRepository.findAllByAnimeIds(animeIds)

    override fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): AnimeMetadataLink? =
        // Check whether this metadata entry is already linked before creating a new link
        linkRepository.findByPlatformAndPlatformId(platform, platformId)

    override fun save(link: AnimeMetadataLink): AnimeMetadataLink =
        // Persist the new link between the anime and its metadata platform entry
        linkRepository.save(link)
}
