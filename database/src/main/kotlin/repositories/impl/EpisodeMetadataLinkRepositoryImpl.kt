package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.EpisodeMetadataLinkEntity
import fr.shikkanime.database.entities.EpisodeMetadataLinkTable
import fr.shikkanime.database.repositories.EpisodeMetadataLinkRepository
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.models.EpisodeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeMetadataLinkRepository::class])
internal class EpisodeMetadataLinkRepositoryImpl(
    private val episodeRepository: EpisodeRepository
) : EpisodeMetadataLinkRepository() {

    /**
     * Finds one metadata link of an episode by its episode, platform and platform identifier.
     *
     * @param episodeId identifier of the episode owning the link.
     * @param platform metadata platform of the link.
     * @param platformId identifier of the episode on the metadata platform.
     * @return the matching entity, or `null` when the link is not known yet.
     * @see [EpisodeMetadataLink]
     */
    private fun findByEpisodeIdPlatformAndPlatformId(
        episodeId: Uuid,
        platform: MetadataPlatform,
        platformId: String
    ): EpisodeMetadataLinkEntity? =
        EpisodeMetadataLinkEntity.find {
            (EpisodeMetadataLinkTable.episode eq episodeId) and
                (EpisodeMetadataLinkTable.platform eq platform) and
                (EpisodeMetadataLinkTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()

    override fun findAllModels(): List<EpisodeMetadataLink> =
        EpisodeMetadataLinkEntity.all()
            .map(EpisodeMetadataLinkEntity::toModel)

    override fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeMetadataLink>> =
        EpisodeMetadataLinkEntity.find { EpisodeMetadataLinkTable.episode inList episodeIds }
            .map(EpisodeMetadataLinkEntity::toModel)
            .groupBy(EpisodeMetadataLink::episodeId)

    override fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): EpisodeMetadataLink? =
        EpisodeMetadataLinkEntity.find {
            (EpisodeMetadataLinkTable.platform eq platform) and
                (EpisodeMetadataLinkTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()
            ?.toModel()

    override fun save(link: EpisodeMetadataLink): EpisodeMetadataLink =
        findByEpisodeIdPlatformAndPlatformId(
            requireNotNull(link.episodeId),
            link.platform,
            link.platformId
        )
            .newIfNotExists(link.id) {
                // The row belongs to the episode its metadata link already references
                this.episode = episodeRepository.getReference(requireNotNull(link.episodeId))
                platform = link.platform
                platformId = link.platformId
                platformAnimeId = link.platformAnimeId
            }
            .applyFlush()
            .toModel()
}
