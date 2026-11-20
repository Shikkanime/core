package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.exposed.TransactionalProxy
import fr.shikkanime.models.EpisodeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import kotlin.uuid.Uuid

/**
 * Application service persisting the metadata links of an episode.
 *
 * Each method runs inside a single database transaction and never recomputes editorial data:
 * what is saved is what is given. Consume it through [TransactionalProxy] or the Koin bindings
 * so the [Transactional] methods are actually intercepted.
 *
 * @see [EpisodeMetadataLink]
 */
interface EpisodeMetadataLinkService {
    /**
     * Loads every metadata link of several episodes.
     *
     * @param episodeIds identifiers of the episodes owning the links.
     * @return the metadata links of each episode, grouped by owner identifier.
     * @see [EpisodeMetadataLink]
     */
    @Transactional
    fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeMetadataLink>>

    /**
     * Finds one metadata link by its platform and platform identifier.
     *
     * @param platform metadata platform of the link.
     * @param platformId identifier of the episode on the metadata platform.
     * @return the matching link, or `null` when it is not known yet.
     * @see [EpisodeMetadataLink]
     */
    @Transactional
    fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): EpisodeMetadataLink?

    /**
     * Upserts one metadata link of an episode.
     *
     * An already known link is left untouched and a new one is inserted; nothing is deleted.
     *
     * @param link metadata link to persist; its `episodeId` must reference the episode.
     * @return the link as stored in database.
     * @see [EpisodeMetadataLink]
     */
    @Transactional
    fun save(link: EpisodeMetadataLink): EpisodeMetadataLink
}
