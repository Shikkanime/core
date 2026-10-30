package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.EpisodePlatformEntity
import fr.shikkanime.models.EpisodePlatform
import kotlin.uuid.Uuid

/**
 * Persistence port of the [EpisodePlatform] releases of an episode, keyed by platform identifier.
 *
 * @see [EpisodeRepository]
 */
internal abstract class EpisodePlatformRepository :
    ModelRepository<Uuid, EpisodePlatformEntity, EpisodePlatform>(EpisodePlatformEntity) {
    /**
     * Loads every platform release of several episodes with one query, grouped by owner.
     *
     * The aggregate composition must not run one lookup per episode: a whole page resolves its
     * releases through this single call.
     *
     * @param episodeIds identifiers of the episodes owning the releases.
     * @return the platform releases of each episode, an empty list for an episode without release.
     * @see [EpisodePlatform]
     */
    abstract fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodePlatform>>

    /**
     * Upserts one platform release of an episode, keyed by platform and platform identifier.
     *
     * A known release is refreshed in place (URL, audio tracks, censorship and release date), a new
     * one is inserted with the supplied identifier. Nothing is deleted.
     *
     * @param episodePlatform platform release to persist; its `episodeId` must reference the episode.
     * @return the release as stored in database.
     * @see [EpisodePlatform]
     */
    abstract fun save(episodePlatform: EpisodePlatform): EpisodePlatform
}
