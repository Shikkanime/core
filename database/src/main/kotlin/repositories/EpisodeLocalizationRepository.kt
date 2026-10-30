package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.EpisodeLocalizationEntity
import fr.shikkanime.models.EpisodeLocalization
import kotlin.uuid.Uuid

/**
 * Persistence port of the [EpisodeLocalization] rows of an episode, keyed by locale.
 *
 * @see [EpisodeRepository]
 */
internal abstract class EpisodeLocalizationRepository :
    ModelRepository<Uuid, EpisodeLocalizationEntity, EpisodeLocalization>(EpisodeLocalizationEntity) {
    /**
     * Loads every localization of several episodes with one query, grouped by owner.
     *
     * The aggregate composition must not run one lookup per episode: a whole page resolves its
     * texts through this single call.
     *
     * @param episodeIds identifiers of the episodes owning the localizations.
     * @return the localizations of each episode, an empty list for an episode without text.
     * @see [EpisodeLocalization]
     */
    abstract fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeLocalization>>

    /**
     * Upserts one localization of an episode, keyed by its locale.
     *
     * A known locale is refreshed in place (same row, `updated_at` touched), a new locale is
     * inserted with the supplied identifier. Nothing is deleted.
     *
     * @param localization localized text to persist; its `episodeId` must reference the episode.
     * @return the localization as stored in database.
     * @see [EpisodeLocalization]
     */
    abstract fun save(localization: EpisodeLocalization): EpisodeLocalization
}
