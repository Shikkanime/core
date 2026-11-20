package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.EpisodeMetadataLinkEntity
import fr.shikkanime.models.EpisodeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import kotlin.uuid.Uuid

/**
 * Persistence port of the [EpisodeMetadataLink] source links of an episode, keyed by platform
 * identifier.
 *
 * @see [EpisodeRepository]
 */
internal abstract class EpisodeMetadataLinkRepository :
    ModelRepository<Uuid, EpisodeMetadataLinkEntity, EpisodeMetadataLink>(EpisodeMetadataLinkEntity) {
    /**
     * Loads every metadata link of the database.
     *
     * @return every stored link, in no particular order.
     * @see [EpisodeMetadataLink]
     */
    abstract fun findAllModels(): List<EpisodeMetadataLink>

    /**
     * Loads every metadata link of several episodes with one query, grouped by owner.
     *
     * @param episodeIds identifiers of the episodes owning the links.
     * @return the metadata links of each episode, an empty list for an episode without link.
     * @see [EpisodeMetadataLink]
     */
    abstract fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeMetadataLink>>

    /**
     * Finds one metadata link by its platform and platform identifier.
     *
     * @param platform metadata platform of the link.
     * @param platformId identifier of the episode on the metadata platform.
     * @return the matching link, or `null` when it is not known yet.
     * @see [EpisodeMetadataLink]
     */
    abstract fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): EpisodeMetadataLink?

    /**
     * Upserts one metadata link of an episode, keyed by episode, platform and platform
     * identifier.
     *
     * An already known link is left untouched and a new one is inserted with the supplied
     * identifier. Nothing is deleted.
     *
     * @param link metadata link to persist; its `episodeId` must reference the episode.
     * @return the link as stored in database.
     * @see [EpisodeMetadataLink]
     */
    abstract fun save(link: EpisodeMetadataLink): EpisodeMetadataLink
}
