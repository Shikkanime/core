package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.EpisodeReleaseEntity
import fr.shikkanime.models.EpisodeRelease
import kotlin.uuid.Uuid

/**
 * Persistence port of the [EpisodeRelease] rows of an episode, keyed by platform video and locale.
 *
 * @see [EpisodeRepository]
 */
internal abstract class EpisodeReleaseRepository :
    ModelRepository<Uuid, EpisodeReleaseEntity, EpisodeRelease>(EpisodeReleaseEntity) {
    /**
     * Loads every release of several episodes with one query, grouped by owner.
     *
     * The aggregate composition must not run one lookup per episode: a whole page resolves its
     * releases through this single call.
     *
     * @param episodeIds identifiers of the episodes owning the releases.
     * @return the releases of each episode, an empty list for an episode without release.
     * @see [EpisodeRelease]
     */
    abstract fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeRelease>>

    /**
     * Upserts one release of an episode, keyed by platform, platform identifier and audio locale.
     *
     * A known release is refreshed in place (URL, censorship and release date), a new one is
     * inserted with the supplied identifier. Nothing is deleted.
     *
     * @param release release to persist; its `episodeId` must reference the episode.
     * @return the release as stored in database.
     * @see [EpisodeRelease]
     */
    abstract fun save(release: EpisodeRelease): EpisodeRelease
}
