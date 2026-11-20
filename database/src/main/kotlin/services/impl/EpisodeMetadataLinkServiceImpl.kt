package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.EpisodeMetadataLinkRepository
import fr.shikkanime.database.services.EpisodeMetadataLinkService
import fr.shikkanime.models.EpisodeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeMetadataLinkService::class])
internal class EpisodeMetadataLinkServiceImpl(
    private val linkRepository: EpisodeMetadataLinkRepository
) : EpisodeMetadataLinkService {

    override fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeMetadataLink>> =
        // Resolve every metadata link of the page in one grouped read
        linkRepository.findAllByEpisodeIds(episodeIds)

    override fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): EpisodeMetadataLink? =
        // Check whether this metadata entry is already linked before creating a new link
        linkRepository.findByPlatformAndPlatformId(platform, platformId)

    override fun save(link: EpisodeMetadataLink): EpisodeMetadataLink =
        // Persist the new link between the episode and its metadata platform entry
        linkRepository.save(link)
}
